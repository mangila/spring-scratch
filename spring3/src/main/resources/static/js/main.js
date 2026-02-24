"use strict";
(() => {
  // node_modules/htmx.org/dist/htmx.esm.js
  var htmx = (() => {
    const HCON = {
      /**
       * Parses an HCON string into an object.
       *
       * @example
       * HCON.parse('foo:1 bar:true');     // {foo: 1, bar: true}
       * HCON.parse('sse.mode:once');      // {sse: {mode: 'once'}}
       * HCON.parse('{"foo": 1}');         // {foo: 1}
       */
      parse(string) {
        if (!string) return {};
        if (string.startsWith("{")) return JSON.parse(string);
        let pattern = /(?:"([^"]+)"|'([^']+)'|([^\s,:]+))(?:\s*:\s*(?:"([^"]*)"|'([^']*)'|<((?:[^/]|\/(?!>))+)\/>|([^\s,]+)))?(?=\s|,|$)/g;
        let result = {};
        for (let match of string.matchAll(pattern)) {
          let [
            ,
            doubleQuotedKey,
            // "key"
            singleQuotedKey,
            // 'key'
            bareKey,
            //  key
            doubleQuotedValue,
            // "value"
            singleQuotedValue,
            // 'value'
            hyperscriptValue,
            // <value/>
            bareValue
            //  value
          ] = match;
          let key = doubleQuotedKey ?? singleQuotedKey ?? bareKey;
          let value = (doubleQuotedValue ?? singleQuotedValue ?? hyperscriptValue ?? bareValue ?? "true").trim();
          try {
            value = JSON.parse(value);
          } catch {
          }
          let isDottedPath = bareKey?.includes(".");
          let pair = isDottedPath ? key.split(".").reduceRight((acc, segment) => ({ [segment]: acc }), value) : { [key]: value };
          HCON.merge(pair, result);
        }
        return result;
      },
      /**
       * Splits an HCON-aware string at top-level commas.
       * Commas inside [], (), <.../>, "...", '...' are preserved.
       *
       * @example
       * HCON.split('a:1, b:2');                // ['a:1', ' b:2']
       * HCON.split('from:".a, .b", click');    // ['from:".a, .b"', ' click']
       */
      split(string) {
        return string.split(/,(?![^\[]*\])(?![^(]*\))(?![^<]*\/>)(?=(?:[^"']|"[^"]*"|'[^']*')*$)/);
      },
      /**
       * Deep-merges a source (HCON string or object) into a target.
       *
       * @example
       * HCON.merge({a: {b: 1}}, {a: {c: 2}});   // {a: {b: 1, c: 2}}
       * HCON.merge('a.b:1', {a: {c: 2}});       // {a: {b: 1, c: 2}}
       */
      merge(source, target) {
        if (typeof source === "string") source = HCON.parse(source);
        for (let [key, val] of Object.entries(source)) {
          if (["__proto__", "constructor", "prototype"].includes(key)) continue;
          let sourceIsObject = val?.constructor === Object;
          let targetIsObject = target[key]?.constructor === Object;
          if (sourceIsObject && targetIsObject) {
            HCON.merge(val, target[key]);
          } else {
            target[key] = val;
          }
        }
        return target;
      }
    };
    class RequestQueue {
      #current = null;
      #queue = [];
      /** Returns "run", "queued", or "dropped" */
      admit(strategy, runRequest, abortRequest) {
        if (!this.#current) {
          this.#current = { strategy, abort: abortRequest };
          return "run";
        }
        if (strategy === "replace" || strategy !== "abort" && this.#current.strategy === "abort") {
          this.#queue = [];
          this.#current.abort?.();
          this.#current = { strategy, abort: abortRequest };
          return "run";
        }
        if (strategy === "queue all") {
          this.#queue.push(runRequest);
        } else if (strategy === "queue last") {
          this.#queue = [runRequest];
        } else if (strategy !== "abort" && strategy !== "drop" && this.#queue.length === 0) {
          this.#queue.push(runRequest);
        } else {
          return "dropped";
        }
        return "queued";
      }
      continue() {
        this.#current = null;
        this.#queue.shift()?.();
      }
      abort() {
        this.#current?.abort?.();
      }
    }
    class Htmx {
      #HCON = HCON;
      #extMethods = /* @__PURE__ */ new Map();
      #approvedExt = "";
      #registeredExt = /* @__PURE__ */ new Set();
      _loc = window.location;
      // mockable for testing
      #internalAPI;
      #Function = Function;
      #AsyncFunction = Object.getPrototypeOf(async function() {
      }).constructor;
      #ttPolicy = { createHTML: (s) => s, createScript: (s) => s };
      #actionSelector;
      #boostSelector = "a,form";
      #verbs = ["get", "post", "put", "patch", "delete", "query"];
      #hxOnQuery;
      #transitionQueue;
      #historyAbort;
      #historyInitialized;
      #processingTransition;
      constructor() {
        this.#initHtmxConfig();
        this.#initRequestIndicatorCss();
        this.#actionSelector = this.#prefixSelector("[hx-action],[hx-get],[hx-post],[hx-put],[hx-patch],[hx-delete],[hx-query]");
        this.#hxOnQuery = new XPathEvaluator().createExpression(`.//*[@*[${this.#prefixes("hx-on").map((p) => `starts-with(name(), "${p}")`).join(" or ")}]]`);
        this.#internalAPI = {
          HCON,
          attributeValue: this.#attributeValue.bind(this),
          parseTriggerSpecs: this.#parseTriggerSpecs.bind(this),
          determineMethodAndAction: this.#determineMethodAndAction.bind(this),
          createRequestContext: this.#createRequestContext.bind(this),
          collectFormData: this.#collectFormData.bind(this),
          getAttributeObject: this.#getAttributeObject.bind(this),
          insertContent: this.#insertContent.bind(this),
          morph: this.#morph.bind(this),
          isSoftMatch: this.#isSoftMatch.bind(this),
          initSecurity: (ttPolicy, syncFn, asyncFn) => {
            if (ttPolicy) this.#ttPolicy = ttPolicy;
            if (syncFn) this.#Function = syncFn;
            if (asyncFn) this.#AsyncFunction = asyncFn;
          },
          onTrigger: this.#onTrigger.bind(this),
          htmxProp: this.#htmxProp.bind(this),
          triggerHtmxEvent: this.#trigger.bind(this),
          executeJavaScript: this.#executeJavaScript.bind(this)
        };
        let init = () => this.initialize();
        if (document.readyState === "loading") {
          document.addEventListener("DOMContentLoaded", init);
        } else {
          setTimeout(init);
        }
      }
      #initHtmxConfig() {
        this.version = "4.0.0";
        this.config = {
          logAll: false,
          prefix: "data-hx-",
          transitions: false,
          history: true,
          mode: "same-origin",
          defaultSwap: "innerHTML",
          defaultFocusScroll: false,
          indicatorClass: "htmx-indicator",
          requestClass: "htmx-request",
          includeIndicatorCSS: true,
          defaultTimeout: 6e4,
          /* 60 second default timeout */
          extensions: "",
          morphIgnore: ["data-htmx-powered"],
          morphSkip: "[hx-morph-skip]",
          morphSkipChildren: "[hx-morph-skip-children]",
          morphScanLimit: 10,
          noSwap: [204, 304],
          implicitInheritance: false,
          defaultSettleDelay: 1,
          allowEmptySwapAfterOOB: false
        };
        let metaConfig = document.querySelector('meta[name="htmx-config"]');
        if (metaConfig) {
          HCON.merge(metaConfig.content, this.config);
        }
        this.#approvedExt = this.config.extensions;
      }
      #initRequestIndicatorCss() {
        if (this.config.includeIndicatorCSS !== false) {
          let indicator = this.config.indicatorClass;
          let request = this.config.requestClass;
          let sheet = new CSSStyleSheet();
          sheet.replaceSync(
            `.${indicator}{opacity:0;visibility: hidden} .${request} .${indicator}, .${request}.${indicator}{opacity:1;visibility: visible;transition: opacity 200ms ease-in}`
          );
          document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet];
        }
      }
      registerExtension(name, extension) {
        if (this.#approvedExt && !this.#approvedExt.split(/,\s*/).includes(name)) return false;
        if (this.#registeredExt.has(name)) return false;
        this.#registeredExt.add(name);
        if (extension.init) extension.init(this.#internalAPI);
        Object.entries(extension).forEach(([key, value]) => {
          if (!this.#extMethods.get(key)?.push(value)) this.#extMethods.set(key, [value]);
        });
      }
      #ignore(elt) {
        let p = this.config.prefix;
        return !elt.closest || elt.closest("[hx-ignore]") != null || p && elt.closest(`[${p}ignore]`) != null;
      }
      #attr(elt, name) {
        let p = this.config.prefix;
        return elt.getAttribute(name) ?? (p ? elt.getAttribute(name.replace("hx-", p)) : null);
      }
      #attrName(elt, name) {
        let p = this.config.prefix && name.replace("hx-", this.config.prefix);
        return elt.hasAttribute(name) ? name : p && elt.hasAttribute(p) ? p : null;
      }
      #prefixSelector(s) {
        return this.#prefixes(s).join(",");
      }
      #prefixes(s) {
        let result = [s];
        if (this.config.prefix) result.push(s.replaceAll("hx-", this.config.prefix));
        return result;
      }
      #queryEltAndDescendants(elt, selector) {
        let results = [...elt.querySelectorAll?.(selector) ?? []];
        if (elt.matches?.(selector)) {
          results.unshift(elt);
        }
        return results;
      }
      #normalizeSwapStyle(style) {
        return style === "before" ? "beforebegin" : style === "after" ? "afterend" : style === "prepend" ? "afterbegin" : style === "append" ? "beforeend" : style;
      }
      #findThisElements(elt, attrName) {
        let result = [];
        this.#attributeValue(elt, attrName, void 0, (val, elt2) => {
          if (val?.split(/\s*[,:]\s*/).includes("this")) result.push(elt2);
        });
        return result;
      }
      #attributeValue(elt, name, defaultVal, eltCollector) {
        name = this.#maybeAdjustMetaCharacter(name);
        let inherited = this.#maybeAdjustMetaCharacter(":inherited");
        let append = this.#maybeAdjustMetaCharacter(":append");
        let val = this.#attr(elt, name) ?? this.#attr(elt, name + inherited);
        if (val != null) return eltCollector ? eltCollector(val, elt) : val;
        let n1 = CSS.escape(this.config.implicitInheritance ? name : name + inherited);
        let n2 = CSS.escape(name + inherited + append);
        let inheritSelector = this.#prefixSelector(`[${n1}],[${n2}]`);
        let appendName = this.#attrName(elt, name + append) ?? this.#attrName(elt, name + inherited + append);
        if (appendName) {
          let appendValue = elt.getAttribute(appendName);
          let parent2 = elt.parentNode?.closest?.(inheritSelector);
          if (eltCollector) eltCollector(appendValue, elt);
          if (parent2) {
            let parentVal = this.#attributeValue(parent2, name, void 0, eltCollector);
            return parentVal ? (parentVal + "," + appendValue).replace(/[{}]/g, "") : appendValue;
          }
          return appendValue;
        }
        let parent = elt.parentNode?.closest?.(inheritSelector);
        if (parent) {
          val = this.#attributeValue(parent, name, void 0, eltCollector);
          if (!eltCollector && val && this.config.implicitInheritance) {
            this.#triggerExtensions(elt, "htmx:after:implicitInheritance", { elt, name, parent });
          }
          return val;
        }
        return defaultVal;
      }
      #parseTriggerSpecs(spec) {
        return HCON.split(spec).flatMap((s) => {
          let [, name, rest] = s.match(/^\s*(\S+\[[^\]]*\]|\S+)\s*(.*?)\s*$/) ?? [];
          if (!name) return [];
          if (/\[[^\]]*$/.test(name)) throw "unterminated:" + name;
          return [{ name, ...HCON.parse(rest) }];
        });
      }
      #determineMethodAndAction(elt, evt) {
        let method = this.#attributeValue(elt, "hx-method");
        let action = this.#attributeValue(elt, "hx-action");
        if (!action) {
          for (let verb of this.#verbs) {
            let verbAction = this.#attributeValue(elt, "hx-" + verb);
            if (verbAction != null) {
              action = verbAction;
              method = verb;
              break;
            }
          }
        }
        if (this.#isBoosted(elt)) {
          action ||= evt.submitter?.getAttribute?.("formAction") || elt.getAttribute(elt.matches("a") ? "href" : "action");
        }
        method ||= evt.submitter?.getAttribute?.("formmethod") || elt.getAttribute("method") || "GET";
        return { action, method: method.toUpperCase() };
      }
      #htmxProp(elt) {
        if (!elt._htmx) {
          elt._htmx = { listeners: [], triggerSpecs: [] };
          elt.setAttribute("data-htmx-powered", "true");
        }
        return elt._htmx;
      }
      #htmxState(elt) {
        return elt._htmx_state ||= {};
      }
      #initializeElement(elt) {
        if (this.#shouldInitialize(elt) && this.#trigger(elt, "htmx:before:init", {}, true)) {
          let htmxProp = this.#htmxProp(elt);
          htmxProp.initialized = true;
          htmxProp.eventHandler = this.#createHtmxEventHandler(elt);
          this.#initializeTriggers(elt);
          this.#trigger(elt, "htmx:after:init", {}, true);
        }
      }
      #createHtmxEventHandler(elt) {
        return async (evt) => {
          try {
            let ctx = this.#createRequestContext(elt, evt);
            await this.#handleTriggerEvent(ctx);
          } catch (e) {
            this.#trigger(elt, "htmx:error", { error: e });
          }
        };
      }
      #createRequestContext(sourceElement, sourceEvent) {
        let { action, method } = this.#determineMethodAndAction(sourceElement, sourceEvent);
        let [fullAction, anchor] = (action || "").split("#");
        let ac = new AbortController();
        let ctx = {
          sourceElement,
          sourceEvent,
          status: "created",
          select: this.#attributeValue(sourceElement, "hx-select"),
          selectOOB: this.#attributeValue(sourceElement, "hx-select-oob"),
          target: this.#attributeValue(sourceElement, "hx-target"),
          swap: this.#attributeValue(sourceElement, "hx-swap") ?? this.config.defaultSwap,
          push: this.#attributeValue(sourceElement, "hx-push-url"),
          replace: this.#attributeValue(sourceElement, "hx-replace-url"),
          transition: this.config.transitions,
          confirm: this.#attributeValue(sourceElement, "hx-confirm"),
          request: {
            validate: "true" === this.#attributeValue(sourceElement, "hx-validate", sourceElement.matches("form") && !sourceElement.noValidate && !sourceEvent.submitter?.formNoValidate ? "true" : "false"),
            action: fullAction,
            anchor,
            method,
            headers: this.#createCoreHeaders(sourceElement),
            abort: ac.abort.bind(ac),
            credentials: "same-origin",
            signal: ac.signal,
            mode: this.config.mode
          }
        };
        if (sourceElement._htmx?.boosted) {
          HCON.merge(sourceElement._htmx.boosted, ctx);
        }
        ctx.target = this.#resolveTarget(sourceElement, ctx.target);
        if (ctx.target) {
          ctx.request.headers["HX-Target"] = this.#buildIdentifier(ctx.target);
        }
        let hxConfig = this.#attributeValue(sourceElement, "hx-config");
        if (hxConfig) {
          HCON.merge(hxConfig, ctx.request);
          ctx.request.mode = this.config.mode;
        }
        return ctx;
      }
      #buildIdentifier(elt) {
        return `${elt.tagName.toLowerCase()}${elt.id ? "#" + encodeURI(elt.id) : ""}`;
      }
      #createCoreHeaders(elt) {
        let headers = {
          "HX-Request": "true",
          "HX-Source": this.#buildIdentifier(elt),
          "HX-Current-URL": location.href,
          "Accept": "text/html"
        };
        if (this.#isBoosted(elt)) {
          headers["HX-Boosted"] = "true";
        }
        return headers;
      }
      #handleHxHeaders(elt, ctx) {
        return this.#getAttributeObject(elt, "hx-headers", (obj) => {
          for (let key in obj) ctx.request.headers[key] = String(obj[key]);
        }, { ctx });
      }
      #resolveTarget(elt, selector) {
        if (selector instanceof Element) {
          return selector;
        } else if (selector != null) {
          return this.#findOrWarn(elt, selector, "hx-target");
        } else if (this.#isBoosted(elt)) {
          return document.body;
        } else {
          return elt;
        }
      }
      #isBoosted(elt) {
        return elt?._htmx?.boosted;
      }
      async #handleTriggerEvent(ctx) {
        let elt = ctx.sourceElement;
        let evt = ctx.sourceEvent;
        if (!elt.isConnected) return;
        if (this.#isModifierKeyClick(evt)) return;
        if (this.#shouldCancel(evt)) evt.preventDefault();
        let usesQueryParams = /GET|DELETE/.test(ctx.request.method);
        let form = usesQueryParams ? elt.matches("form") ? elt : null : elt.form || elt.closest("form");
        let body = this.#collectFormData(elt, form, evt.submitter, ctx.request.validate, usesQueryParams);
        if (!body) return;
        let valsResult = this.#getAttributeObject(elt, "hx-vals", (obj) => {
          ctx.vals = obj;
          for (let key in obj) body.set(key, obj[key]);
        }, { ctx });
        if (valsResult) await valsResult;
        if (ctx.values) {
          for (let k in ctx.values) {
            body.delete(k);
            body.append(k, ctx.values[k]);
          }
        }
        let headersResult = this.#handleHxHeaders(elt, ctx);
        if (headersResult) await headersResult;
        Object.assign(ctx.request, {
          form,
          submitter: evt.submitter,
          body
        });
        if (!this.#trigger(elt, "htmx:config:request", { ctx })) return;
        if (ctx.request.method === "DIALOG") return;
        let javascriptContent = this.#extractJavascriptContent(ctx.request.action);
        if (javascriptContent != null) {
          let data2 = Object.fromEntries(ctx.request.body);
          await this.#executeJavaScript(ctx.sourceElement, data2, javascriptContent, false);
          return;
        } else if (usesQueryParams) {
          let url = new URL(ctx.request.action, document.baseURI);
          for (let key of ctx.request.body.keys()) {
            url.searchParams.delete(key);
          }
          for (let [key, value] of ctx.request.body) {
            url.searchParams.append(key, value);
          }
          if (url.origin === location.origin) {
            ctx.request.action = url.pathname + url.search;
          } else {
            ctx.request.action = url.href;
          }
          ctx.request.body = null;
        } else if ((this.#attributeValue(elt, "hx-encoding") ?? form?.enctype) !== "multipart/form-data") {
          ctx.request.body = new URLSearchParams(ctx.request.body);
        }
        await this.#issueRequest(ctx);
      }
      async #issueRequest(ctx) {
        let elt = ctx.sourceElement;
        let syncStrategy = this.#determineSyncStrategy(elt);
        let requestQueue = this.#getRequestQueue(elt);
        this.#initializeAbortListener(elt);
        if (requestQueue.admit(
          syncStrategy,
          () => this.#issueRequest(ctx),
          // run when ready
          () => ctx.request?.abort?.()
          // abort if replaced
        ) !== "run") return;
        ctx.status = "issuing";
        let indicators = [];
        let disableElements = [];
        try {
          if (ctx.confirm) {
            let confirmed = await new Promise((resolve) => {
              let detail = { ctx, issueRequest: () => resolve(true), dropRequest: () => resolve(false) };
              if (this.#trigger(elt, "htmx:confirm", detail)) {
                let js = this.#extractJavascriptContent(ctx.confirm);
                resolve(js ? this.#executeJavaScript(elt, { ctx }, js, true) : window.confirm(ctx.confirm));
              }
            });
            if (!confirmed) return;
          }
          this.#initTimeout(ctx);
          indicators = this.#showIndicators(elt);
          disableElements = this.#disableElements(elt);
          ctx.fetch ||= window.fetch.bind(window);
          ctx.request.headers["HX-Request-Type"] = ctx.target === document.body || ctx.select ? "full" : "partial";
          if (!this.#trigger(elt, "htmx:before:request", { ctx })) return;
          let response = await ctx.fetch(ctx.request.action, ctx.request);
          ctx.response = {
            raw: response,
            status: response.status,
            headers: response.headers
          };
          this.#extractHxHeaders(ctx);
          if (!this.#trigger(elt, "htmx:before:response", { ctx })) return;
          ctx.text = await response.text();
          if (!this.#trigger(elt, "htmx:after:request", { ctx })) return;
          if (ctx.response.status >= 400) {
            this.#trigger(elt, "htmx:response:error", { ctx });
          }
          if (this.#handleHeadersAndMaybeReturnEarly(ctx)) {
            ctx.keepIndicators = true;
            return;
          }
          if (ctx.status === "issuing") {
            if (ctx.hx.retarget) ctx.target = ctx.hx.retarget;
            if (ctx.hx.reswap) ctx.swap = ctx.hx.reswap;
            if (ctx.hx.reselect) ctx.select = ctx.hx.reselect;
            ctx.status = "response received";
            this.#handleStatusCodes(ctx);
            await this.swap(ctx);
            ctx.status = "swapped";
          }
        } catch (error2) {
          ctx.status = "error: " + error2;
          this.#trigger(elt, "htmx:error", { ctx, error: error2 });
        } finally {
          await ctx.extensionPromise?.catch(() => {
          });
          clearTimeout(ctx.requestTimeout);
          if (ctx.hx?.trigger) {
            this.#handleTriggerHeader(ctx.hx.trigger, ctx.sourceElement);
          }
          this.#trigger(elt, "htmx:finally:request", { ctx });
          if (!ctx.keepIndicators) {
            this.#hideIndicators(indicators);
            this.#enableElements(disableElements);
          }
          requestQueue.continue();
        }
      }
      // Extract HX-* response headers into ctx.hx
      // Maps: HX-Trigger -> ctx.hx.trigger, HX-Push-Url -> ctx.hx.pushurl, etc.
      #extractHxHeaders(ctx) {
        ctx.hx = {};
        for (let [k, v] of ctx.response.raw.headers) {
          if (k.toLowerCase().startsWith("hx-")) {
            ctx.hx[k.slice(3).toLowerCase().replace(/-/g, "")] = v;
          }
        }
      }
      // Handle response headers that abort normal swap processing.
      // Returns true if the response was fully handled by a header.
      #handleHeadersAndMaybeReturnEarly(ctx) {
        if (ctx.hx.refresh === "true") {
          this._loc.reload();
          return true;
        }
        if (ctx.hx.redirect) {
          this._loc.href = ctx.hx.redirect;
          return true;
        }
        if (ctx.hx.location) {
          let path = ctx.hx.location, opts = {};
          let parsed = HCON.parse(path);
          if (path[0] === "{" || parsed.path != null) {
            opts = parsed;
            path = opts.path;
            delete opts.path;
          }
          if (opts.push == null && opts.replace == null) opts.push = "true";
          this.ajax("GET", path, opts);
          return true;
        }
      }
      #initTimeout(ctx) {
        let timeout = ctx.request.timeout != null ? this.parseInterval(ctx.request.timeout) : this.config.defaultTimeout;
        if (timeout) {
          ctx.requestTimeout = setTimeout(() => ctx.request?.abort?.(), timeout);
        }
      }
      #determineSyncStrategy(elt) {
        let hxSync = this.#attributeValue(elt, "hx-sync");
        if (!hxSync) return "queue first";
        let strategy = hxSync.split(":").pop().trim();
        return /^(drop|abort|replace|queue)/.test(strategy) ? strategy : "queue first";
      }
      #getRequestQueue(elt) {
        let hxSync = this.#attributeValue(elt, "hx-sync");
        let syncElt = elt;
        if (hxSync) {
          let selector = hxSync.includes(":") ? hxSync.slice(0, hxSync.lastIndexOf(":")).trim() : /^(drop|abort|replace|queue)/.test(hxSync) ? null : hxSync;
          if (selector) syncElt = this.#findOrWarn(elt, selector, "hx-sync") || elt;
        }
        return this.#htmxState(syncElt).rq ||= new RequestQueue();
      }
      #isModifierKeyClick(evt) {
        return evt.type === "click" && (evt.ctrlKey || evt.metaKey || evt.shiftKey) && !!evt.currentTarget?.closest?.("a[href]");
      }
      #shouldCancel(evt) {
        let elt = evt.currentTarget;
        let isSubmit = evt.type === "submit" && elt?.tagName === "FORM";
        if (isSubmit) return true;
        let isClick = evt.type === "click" && evt.button === 0;
        if (!isClick) return false;
        let btn = elt?.closest?.('button, input[type="submit"], input[type="image"]');
        let form = btn?.form || btn?.closest("form");
        let isSubmitButton = btn && !btn.disabled && form && (btn.type === "submit" || btn.type === "image" || !btn.type && btn.tagName === "BUTTON");
        if (isSubmitButton) return true;
        let link = elt?.closest?.("a");
        if (!link || !link.href) return false;
        let href = link.getAttribute("href");
        let isFragmentOnly = href && href.startsWith("#") && href.length > 1;
        return !isFragmentOnly;
      }
      #initializeTriggers(elt, initialHandler = elt._htmx.eventHandler) {
        let hxTrigger = this.#attributeValue(elt, "hx-trigger");
        let trigger2 = hxTrigger || (elt.matches("form") ? "submit" : elt.matches("input:not([type=button]):not([type=submit]),select,textarea") ? "change" : "click");
        this.#onTrigger(elt, trigger2, initialHandler);
      }
      // Wire up event listeners with full modifier support (once, prevent, stop,
      // delay, throttle, changed, capture, passive, from, filter, etc.)
      #onTrigger(elt, specString, handler4) {
        let specs = this.#parseTriggerSpecs(specString);
        this.#htmxProp(elt).triggerSpecs.push(...specs);
        for (let spec of specs) {
          spec.listeners = [];
          let [eventName, filter] = this.#extractFilter(spec.name);
          let fromElts = [elt];
          if (spec.from === "outside") fromElts = [document];
          else if (spec.from && spec.from !== "self") fromElts = this.#findAllExt(elt, spec.from);
          let inner = (evt) => {
            if (spec.halt || spec.prevent) evt.preventDefault();
            if (spec.halt || spec.stop || spec.consume) evt.stopPropagation();
            if (spec.once) {
              for (let info of spec.listeners) info.fromElt.removeEventListener(info.eventName, info.handler, info);
            }
            handler4(evt);
          };
          let timed = inner;
          if (spec.delay) {
            timed = (evt) => {
              clearTimeout(spec.timeout);
              spec.timeout = setTimeout(() => inner(evt), this.parseInterval(spec.delay));
            };
          } else if (spec.throttle) {
            timed = (evt) => {
              if (spec.throttled) {
                spec.throttledEvent = evt;
              } else {
                spec.throttled = true;
                inner(evt);
                spec.throttleTimeout = setTimeout(() => {
                  spec.throttled = false;
                  if (spec.throttledEvent) {
                    let e = spec.throttledEvent;
                    spec.throttledEvent = null;
                    timed(e);
                  }
                }, this.parseInterval(spec.throttle));
              }
            };
          }
          spec.handler = (evt) => {
            if (spec.from === "self" && evt.target !== elt) return;
            if (spec.from === "outside" && elt.contains(evt.target)) return;
            if (spec.target && !evt.target?.matches?.(spec.target)) return;
            if (spec.changed) {
              let values = spec.values ??= /* @__PURE__ */ new WeakMap();
              let changed = false;
              for (let fromElt of fromElts) {
                if (values.get(fromElt) !== fromElt.value) {
                  changed = true;
                  values.set(fromElt, fromElt.value);
                }
              }
              if (!changed) return;
            }
            if (filter) {
              if (this.#shouldCancel(evt)) evt.preventDefault();
              let evtArgs = {};
              for (let k in evt) evtArgs[k] = evt[k];
              if (!this.#executeJavaScript(elt, evtArgs, filter, true, false)) return;
            }
            timed(evt);
          };
          if (eventName === "intersect" || eventName === "revealed") {
            let observerOptions = { rootMargin: spec.rootMargin };
            if (spec.root) observerOptions.root = this.#findOrWarn(elt, spec.root);
            if (spec.threshold) observerOptions.threshold = parseFloat(spec.threshold);
            let isRevealed = eventName === "revealed";
            spec.observer = new IntersectionObserver((entries) => {
              for (let i = 0; i < entries.length; i++) {
                if (entries[i].isIntersecting) {
                  this.trigger(elt, "intersect", {}, false);
                  if (isRevealed) spec.observer.disconnect();
                  break;
                }
              }
            }, observerOptions);
            eventName = "intersect";
            spec.observer.observe(elt);
          }
          if (eventName === "every") {
            let interval = Object.keys(spec).find((k) => k !== "name");
            spec.interval = setInterval(() => {
              if (elt.isConnected) this.#trigger(elt, "every", {}, false);
              else clearInterval(spec.interval);
            }, this.parseInterval(interval));
          }
          if (eventName === "load") {
            spec.handler(new CustomEvent("load"));
            continue;
          }
          for (let fromElt of fromElts) {
            let listenerInfo = {
              fromElt,
              eventName,
              handler: spec.handler,
              capture: !!spec.capture,
              passive: !!spec.passive
            };
            elt._htmx.listeners.push(listenerInfo);
            spec.listeners.push(listenerInfo);
            fromElt.addEventListener(eventName, spec.handler, listenerInfo);
          }
        }
      }
      #extractFilter(str) {
        let match = str.match(/^([^\[]*)\[([^\]]*)]/);
        if (!match) return [str, null];
        return [match[1], match[2]];
      }
      #handleTriggerHeader(value, elt) {
        if (value[0] === "{") {
          let triggers = HCON.parse(value);
          for (let name in triggers) {
            let detail = triggers[name];
            let target = elt;
            if (detail?.target) {
              target = this.find(detail.target);
            }
            this.trigger(target, name, typeof detail === "object" ? detail : { value: detail });
          }
        } else {
          value.split(",").forEach((name) => this.trigger(elt, name.trim(), {}));
        }
      }
      #apiMethods(thisArg) {
        let bound = {};
        let proto = Object.getPrototypeOf(this);
        for (let name of Object.getOwnPropertyNames(proto)) {
          if (name !== "constructor" && typeof this[name] === "function") {
            if (["find", "findAll"].includes(name)) {
              bound[name] = (arg1, arg2) => {
                if (arg2 === void 0) {
                  return this[name](thisArg, arg1);
                } else {
                  return this[name](arg1, arg2);
                }
              };
            } else {
              bound[name] = this[name].bind(this);
            }
          }
        }
        return bound;
      }
      #executeJavaScript(thisArg, obj, code, expression = true, isAsync = true, compile = false) {
        let args = {};
        Object.assign(args, this.#apiMethods(thisArg));
        let scope2 = {};
        let detail = { scope: scope2, code };
        this.#triggerExtensions(thisArg, "htmx:scope", detail);
        code = detail.code;
        Object.assign(args, scope2);
        Object.assign(args, obj);
        let keys = Object.keys(args);
        let values = Object.values(args);
        let FunctionConstructor = isAsync ? this.#AsyncFunction : this.#Function;
        let func = new FunctionConstructor(...keys, expression ? `return (${code})` : code);
        return compile ? () => func.call(thisArg, ...values) : func.call(thisArg, ...values);
      }
      /**
       * Initialize htmx attributes on root and all its descendants. When force is true, root
       * and every powered descendant are first torn down and re-wired from their current
       * attributes - use this after mutating hx-* attributes on an already-processed element.
       * @see https://four.htmx.org/reference/methods/htmx-process
       * @param {Element | ShadowRoot} root
       * @param {boolean} [force]
       */
      process(root, force) {
        if (!root?.isConnected) return;
        if (!(root instanceof Element)) {
          for (let elt of root.children || []) this.process(elt, force);
          return;
        }
        if (force) this.#cleanup(root, true);
        if (this.#ignore(root)) return;
        if (!this.#trigger(root, "htmx:before:process")) return;
        let hxOnNodes = [root];
        let iter = this.#hxOnQuery.evaluate(root);
        let node = null;
        while (node = iter.iterateNext()) hxOnNodes.push(node);
        for (let hxOnNode of hxOnNodes) {
          if (!this.#ignore(hxOnNode) && this.#trigger(hxOnNode, "htmx:before:on:init", {}, true)) {
            this.#handleHxOnAttributes(hxOnNode);
          }
        }
        for (let elt of this.#queryEltAndDescendants(root, this.#actionSelector)) {
          this.#initializeElement(elt);
        }
        for (let elt of this.#queryEltAndDescendants(root, this.#boostSelector)) {
          this.#maybeBoost(elt);
        }
        this.#trigger(root, "htmx:after:process");
      }
      #maybeBoost(elt) {
        let hxBoost = this.#attributeValue(elt, "hx-boost");
        if (hxBoost && hxBoost !== "false" && this.#shouldBoost(elt) && this.#trigger(elt, "htmx:before:init", {}, true)) {
          let htmxProp = this.#htmxProp(elt);
          htmxProp.initialized = true;
          htmxProp.eventHandler = this.#createHtmxEventHandler(elt);
          htmxProp.boosted = hxBoost;
          let eventName = elt.matches("a") ? "click" : "submit";
          elt._htmx.listeners.push({ fromElt: elt, eventName, handler: elt._htmx.eventHandler });
          elt.addEventListener(eventName, elt._htmx.eventHandler);
          this.#trigger(elt, "htmx:after:init", {}, true);
        }
      }
      #shouldBoost(elt) {
        if (this.#shouldInitialize(elt)) {
          if (elt.tagName === "A") {
            if (elt.target === "" || elt.target === "_self") {
              return !elt.hasAttribute("download") && !elt.getAttribute("href")?.startsWith?.("#") && this.#isSameOrigin(elt.href);
            }
          } else if (elt.tagName === "FORM") {
            return elt.method !== "dialog" && this.#isSameOrigin(elt.action);
          }
        }
      }
      #isSameOrigin(url) {
        try {
          const parsed = new URL(url, window.location.href);
          return parsed.origin === window.location.origin;
        } catch (e) {
          return false;
        }
      }
      #shouldInitialize(elt) {
        return !elt._htmx?.initialized && !this.#ignore(elt);
      }
      /**
       * Remove listeners, timers, and observers from elt and all its powered descendants.
       * When force is true, also delete their htmx state so a re-process fully re-initializes them.
       * @param {Element} elt
       * @param {boolean} [force]
       */
      #cleanup(elt, force) {
        let elts = [elt, ...elt.querySelectorAll?.("[data-htmx-powered]") ?? []];
        for (let e of elts) {
          if (!e._htmx) continue;
          this.#trigger(e, "htmx:before:cleanup");
          for (let spec of e._htmx.triggerSpecs || []) {
            if (spec.interval) clearInterval(spec.interval);
            if (spec.timeout) clearTimeout(spec.timeout);
            if (spec.throttleTimeout) clearTimeout(spec.throttleTimeout);
            spec.observer?.disconnect();
          }
          for (let info of e._htmx.listeners || []) {
            info.fromElt.removeEventListener(info.eventName, info.handler, info);
          }
          e.removeAttribute("data-htmx-powered");
          this.#trigger(e, "htmx:after:cleanup");
          if (force) delete e._htmx;
        }
      }
      #handlePreservedElements(fragment) {
        let pantry = document.createElement("div");
        pantry.hidden = true;
        document.body.insertAdjacentElement("afterend", pantry);
        let newPreservedElts = fragment.querySelectorAll?.(this.#prefixSelector("[hx-preserve]")) || [];
        for (let preservedElt of newPreservedElts) {
          let currentElt = document.getElementById(preservedElt.id);
          if (currentElt) {
            this.#moveBefore(pantry, currentElt, null);
          }
        }
        return pantry;
      }
      #restorePreservedElements(pantry) {
        for (let preservedElt of [...pantry.children]) {
          let newElt = document.getElementById(preservedElt.id);
          if (newElt) {
            this.#moveBefore(newElt.parentNode, preservedElt, newElt);
            this.#cleanup(newElt);
            newElt.remove();
          }
        }
        pantry.remove();
      }
      #parseHTML(resp) {
        let trusted = this.#ttPolicy.createHTML(resp);
        return Document.parseHTMLUnsafe?.(trusted) || new DOMParser().parseFromString(trusted, "text/html");
      }
      #makeFragment(text) {
        let response = text.replace(/<hx-([a-z]+)(\s+|>)/gi, '<template hx type="$1"$2').replace(/<\/hx-[a-z]+>/gi, "</template>");
        let title = "";
        response = response.replace(/<head(\s[^>]*)?>[\s\S]*?<\/head>/i, (m) => (title = this.#parseHTML(m).title, ""));
        let startTag = response.match(/<([a-z][^\/>\x20\t\r\n\f]*)/i)?.[1]?.toLowerCase();
        let doc, fragment;
        if (startTag === "html" || startTag === "body") {
          doc = this.#parseHTML(response);
          fragment = document.createDocumentFragment();
          fragment.append(doc.body);
        } else {
          doc = this.#parseHTML(`<template>${response}</template>`);
          fragment = doc.querySelector("template").content;
        }
        if (!title) {
          let titleElt = fragment.querySelector("title:not(svg title)");
          if (titleElt) {
            title = titleElt.textContent;
            titleElt.remove();
          }
        }
        this.#processScripts(fragment);
        return {
          fragment,
          title
        };
      }
      #createOOBTask(tasks, elt, oobValue, sourceElement) {
        let targetSelector = elt.id ? "#" + CSS.escape(elt.id) : null;
        if (oobValue !== "true" && oobValue && !oobValue.includes(" ")) {
          [oobValue, targetSelector = targetSelector] = oobValue.split(/:(.*)/);
        }
        if (oobValue === "true" || !oobValue) oobValue = "outerHTML";
        let swapSpec = this.#parseSwapSpec(oobValue);
        targetSelector = swapSpec.target || targetSelector;
        swapSpec.strip ??= !swapSpec.style.startsWith("outer");
        if (!targetSelector) return;
        let targets = [...document.querySelectorAll(targetSelector)];
        for (let target of targets) {
          let fragment = document.createDocumentFragment();
          fragment.append(elt.cloneNode(true));
          tasks.push({ type: "oob", fragment, target, swapSpec, sourceElement });
        }
        elt.remove();
      }
      #processOOB(fragment, sourceElement, selectOOB) {
        let tasks = [];
        if (selectOOB) {
          for (let spec of selectOOB.split(",")) {
            let [selector, oobValue = "true"] = spec.split(/:(.*)/);
            for (let elt of fragment.querySelectorAll(selector)) {
              this.#createOOBTask(tasks, elt, oobValue, sourceElement);
            }
          }
        }
        for (let oobElt of fragment.querySelectorAll(this.#prefixSelector("[hx-swap-oob]"))) {
          let oobAttr = this.#attrName(oobElt, "hx-swap-oob");
          let oobValue = oobElt.getAttribute(oobAttr);
          oobElt.removeAttribute(oobAttr);
          this.#createOOBTask(tasks, oobElt, oobValue, sourceElement);
        }
        return tasks;
      }
      #insertNodes(parent, before, fragment) {
        if (before) {
          before.before(...fragment.childNodes);
        } else {
          parent.append(...fragment.childNodes);
        }
      }
      #parseSwapSpec(swapStr) {
        swapStr = swapStr.trim();
        let style = this.config.defaultSwap;
        if (swapStr && !/^\S*:/.test(swapStr)) {
          let m = swapStr.match(/^(\S+)\s*(.*)$/);
          style = m[1];
          swapStr = m[2];
        }
        return { style: this.#normalizeSwapStyle(style), ...HCON.parse(swapStr) };
      }
      #processPartials(fragment, ctx) {
        let tasks = [];
        for (let templateElt of fragment.querySelectorAll("template[hx]")) {
          let type = templateElt.getAttribute("type");
          if (type === "partial") {
            let targetSelector = this.#attr(templateElt, "hx-target") || (templateElt.id ? "#" + CSS.escape(templateElt.id) : null);
            if (targetSelector) {
              this.#processScripts(templateElt.content);
              let swapSpec = this.#parseSwapSpec(this.#attr(templateElt, "hx-swap") || this.config.defaultSwap);
              let targets = this.#findAllExt(ctx.sourceElement, targetSelector);
              for (let target of targets.length ? targets : [null]) {
                tasks.push({
                  type: "partial",
                  fragment: templateElt.content.cloneNode(true),
                  target,
                  swapSpec,
                  sourceElement: ctx.sourceElement
                });
              }
            }
          } else {
            this.#triggerExtensions(templateElt, "htmx:process:" + type, { ctx, tasks });
          }
          templateElt.remove();
        }
        return tasks;
      }
      #setFocus(elt, options, start2, end) {
        try {
          if (start2 != null && elt.setSelectionRange) {
            elt.setSelectionRange(start2, end);
          }
          elt.focus(options);
        } catch (e) {
        }
      }
      #handleAutoFocus(elt) {
        let autofocus = this.#queryEltAndDescendants(elt, "[autofocus]")[0];
        if (autofocus) {
          this.#setFocus(autofocus);
        }
      }
      #handleScroll(swapSpec, target) {
        if (swapSpec.scroll) {
          let scrollTarget = swapSpec.scrollTarget ? this.#findExt(swapSpec.scrollTarget) : target;
          if (scrollTarget) {
            if (swapSpec.scroll === "top") {
              scrollTarget.scrollTop = 0;
            } else if (swapSpec.scroll === "bottom") {
              scrollTarget.scrollTop = scrollTarget.scrollHeight;
            }
          }
        }
        if (swapSpec.show === "top" || swapSpec.show === "bottom") {
          let showTarget = swapSpec.showTarget ? this.#findExt(swapSpec.showTarget) : target;
          showTarget?.scrollIntoView?.(swapSpec.show === "top");
        }
      }
      #handleAnchorScroll(ctx) {
        if (ctx.request?.anchor) {
          document.getElementById(ctx.request.anchor)?.scrollIntoView({ block: "start", behavior: "auto" });
        }
      }
      #processScripts(container) {
        let scripts = this.#queryEltAndDescendants(container, "script");
        for (let oldScript of scripts) {
          let newScript = document.createElement("script");
          for (let attr of oldScript.attributes) {
            newScript.setAttribute(attr.name, attr.value);
          }
          if (this.config.inlineScriptNonce) {
            newScript.nonce = this.config.inlineScriptNonce;
          }
          newScript.textContent = this.#ttPolicy.createScript(oldScript.textContent);
          oldScript.replaceWith(newScript);
        }
      }
      //============================================================================================
      // Public JS API
      //============================================================================================
      initialize() {
        if (this.config.history && !this.#historyInitialized) {
          this.#historyInitialized = true;
          if (!history.state) history.replaceState({ htmx: true }, "", location.href);
          if (window.navigation && !/firefox/i.test(navigator.userAgent)) {
            navigation.addEventListener("navigate", (event) => {
              if (event.navigationType === "traverse" && event.canIntercept && !event.hashChange)
                event.intercept({ handler: () => this.#restoreHistory() });
            });
          } else {
            window.addEventListener("popstate", (event) => this.#restoreHistory(event.state));
          }
        }
        this.process(document.body);
      }
      async swap(ctx) {
        try {
          this.#handleHistoryUpdate(ctx);
          let { fragment, title } = this.#makeFragment(ctx.text);
          ctx.title = title;
          let tasks = [];
          let oobTasks = this.#processOOB(fragment, ctx.sourceElement, ctx.selectOOB);
          let partialTasks = this.#processPartials(fragment, ctx);
          tasks.push(...oobTasks, ...partialTasks);
          let hasPartials = partialTasks.length || oobTasks.length && !this.config.allowEmptySwapAfterOOB;
          let mainSwap = this.#processMainSwap(ctx, fragment, hasPartials);
          if (mainSwap) {
            tasks.unshift(mainSwap);
          }
          if (!this.#trigger(ctx.sourceElement, "htmx:before:swap", { ctx, tasks })) {
            return;
          }
          let swapPromises = [];
          let transitionTasks = [];
          for (let task of tasks) {
            if (task.swapSpec?.transition ?? mainSwap?.transition ?? ctx.transition) {
              transitionTasks.push(task);
            } else {
              swapPromises.push(this.#insertContent(task));
            }
          }
          if (transitionTasks.length > 0) {
            let tasksWrapper = async () => {
              for (let task of transitionTasks) {
                await this.#insertContent(task, false);
              }
            };
            swapPromises.push(this.#submitTransitionTask(tasksWrapper, ctx));
          }
          await Promise.all(swapPromises);
          if (!ctx.sourceElement?.isConnected && mainSwap?.target?.isConnected) {
            ctx.sourceElement = mainSwap.target;
          }
          this.#trigger(ctx.sourceElement, "htmx:after:swap", { ctx });
          if (ctx.title && !mainSwap?.swapSpec?.ignoreTitle) document.title = ctx.title;
          this.#handleAnchorScroll(ctx);
        } finally {
          this.#trigger(ctx.sourceElement, "htmx:finally:swap", { ctx });
        }
      }
      #processMainSwap(ctx, fragment, hasPartials) {
        let swapSpec = this.#parseSwapSpec(ctx.swap || this.config.defaultSwap);
        if (swapSpec.style === "delete" || // delete always runs regardless of content
        fragment.childElementCount > 0 || // or fragment has elements
        fragment.textContent.trim() || // or fragment has text
        (swapSpec.swapEmpty ?? !hasPartials)) {
          if (ctx.select) {
            let selected = fragment.querySelectorAll(ctx.select);
            fragment = document.createDocumentFragment();
            fragment.append(...selected);
          }
          if (this.#isBoosted(ctx.sourceElement)) {
            swapSpec.show ||= "top";
          }
          let mainSwap = {
            type: "main",
            fragment,
            target: this.#resolveTarget(ctx.sourceElement || document.body, swapSpec.target || ctx.target),
            swapSpec,
            sourceElement: ctx.sourceElement,
            transition: ctx.transition && swapSpec.transition !== false
          };
          return mainSwap;
        }
      }
      async #insertContent(task, cssTransition = true) {
        let { target, swapSpec, fragment } = task;
        if (typeof target === "string") {
          target = document.querySelector(target);
        }
        if (!target) return;
        if (typeof swapSpec === "string") {
          swapSpec = this.#parseSwapSpec(swapSpec);
        }
        let swapStyle = swapSpec.style;
        if (swapStyle === "none") return;
        if (fragment.firstElementChild?.tagName === "BODY") {
          const keepBody = target === document.body && swapStyle.startsWith("outer");
          if (keepBody && swapStyle === "outerHTML") swapStyle = "outerSync";
          swapSpec.strip ??= !keepBody;
        }
        if (swapSpec.strip && fragment.firstElementChild) {
          fragment = document.createDocumentFragment();
          fragment.append(...(task.fragment.firstElementChild.content || task.fragment.firstElementChild).childNodes);
        }
        this.#addClass(target, "htmx-swapping");
        if (cssTransition && task.swapSpec?.swap) {
          await this.timeout(task.swapSpec?.swap);
        }
        if (swapStyle === "delete") {
          if (target.parentNode) {
            this.#cleanup(target);
            target.parentNode.removeChild(target);
          }
          return;
        }
        let focusInfo;
        let settleTasks = [];
        let settleDelay = swapSpec.settle ?? this.config.defaultSettleDelay;
        let parentNode = target.parentNode;
        if (swapStyle === "innerHTML" || swapStyle === "outerHTML" && parentNode) {
          let activeElt = document.activeElement;
          if (activeElt?.id) {
            let start2, end;
            try {
              start2 = activeElt.selectionStart;
              end = activeElt.selectionEnd;
            } catch (e) {
            }
            focusInfo = { elt: activeElt, start: start2, end };
          }
          settleTasks = cssTransition && settleDelay ? this.#startCSSTransitions(fragment, target) : [];
        }
        let pantry = this.#handlePreservedElements(fragment);
        let newContent = [...fragment.childNodes];
        try {
          if (swapStyle === "innerHTML") {
            for (const child of target.children) {
              this.#cleanup(child);
            }
            target.replaceChildren(...fragment.childNodes);
          } else if (swapStyle === "textContent") {
            for (const child of target.querySelectorAll("[data-htmx-powered]")) {
              this.#cleanup(child);
            }
            target.textContent = fragment.textContent;
          } else if (swapStyle === "outerHTML") {
            if (parentNode) {
              this.#insertNodes(parentNode, target, fragment);
              this.#cleanup(target);
              parentNode.removeChild(target);
              target = newContent[0] || parentNode;
            }
          } else if (swapStyle === "outerSync") {
            this.#copyAttributes(target, fragment.firstElementChild);
            for (const child of target.children) {
              this.#cleanup(child);
            }
            target.replaceChildren(...fragment.firstElementChild.childNodes);
            newContent = [target];
          } else if (swapStyle === "innerMorph") {
            this.#morph(target, fragment, true);
            newContent = [...target.childNodes];
          } else if (swapStyle === "outerMorph") {
            this.#morph(target, fragment, false);
            newContent.push(target);
          } else if (swapStyle === "beforebegin") {
            if (parentNode) {
              this.#insertNodes(parentNode, target, fragment);
            }
          } else if (swapStyle === "afterbegin") {
            this.#insertNodes(target, target.firstChild, fragment);
          } else if (swapStyle === "beforeend") {
            this.#insertNodes(target, null, fragment);
          } else if (swapStyle === "afterend") {
            if (parentNode) {
              this.#insertNodes(parentNode, target.nextSibling, fragment);
            }
          } else {
            let methods = this.#extMethods.get("handle_swap") || [];
            let handled = false;
            for (const method of methods) {
              let result = method(swapStyle, target, fragment, swapSpec);
              if (result) {
                handled = true;
                if (Array.isArray(result)) {
                  newContent = result;
                }
                break;
              }
            }
            if (!handled) {
              throw new Error(`Unknown swap style: ${swapStyle}`);
            }
          }
        } finally {
          this.#removeClass(target, "htmx-swapping");
        }
        task.target = target;
        this.#restorePreservedElements(pantry);
        if (focusInfo && !focusInfo.elt.matches(":focus")) {
          let newElt = document.getElementById(focusInfo.elt.id);
          if (newElt) {
            let focusOptions = { preventScroll: swapSpec.focusScroll !== void 0 ? !swapSpec.focusScroll : !this.config.defaultFocusScroll };
            this.#setFocus(newElt, focusOptions, focusInfo.start, focusInfo.end);
          }
        }
        this.#trigger(target, "htmx:before:settle", { task, newContent, settleTasks });
        for (const elt of newContent) {
          this.#addClass(elt, "htmx-added");
        }
        if (cssTransition && settleTasks.length > 0) {
          this.#addClass(target, "htmx-settling");
          await this.timeout(settleDelay);
          for (let settleTask of settleTasks) {
            settleTask();
          }
          this.#removeClass(target, "htmx-settling");
        }
        this.#trigger(target, "htmx:after:settle", { task, newContent, settleTasks });
        for (const elt of newContent) {
          this.#removeClass(elt, "htmx-added");
          this.process(elt);
          this.#handleAutoFocus(elt);
        }
        this.#handleScroll(swapSpec, target);
      }
      #trigger(on2, eventName, detail = {}, bubbles = true) {
        if (detail.error) {
          let prefix2 = `htmx: ${eventName}: ${detail.error.message ?? detail.error}`;
          if (detail.error instanceof Error) console.error(prefix2, detail.error, { elt: on2, detail });
          else console.error(prefix2, { elt: on2, detail });
        } else if (detail.warn) {
          console.warn(`htmx: ${eventName}: ${detail.warn}`, { elt: on2, detail });
        } else if (this.config.logAll) {
          console.log(`htmx: ${eventName}`, { elt: on2, detail });
        }
        on2 = this.#normalizeElement(on2);
        this.#triggerExtensions(on2, eventName, detail);
        return this.trigger(on2, this.#maybeAdjustMetaCharacter(eventName), detail, bubbles);
      }
      #triggerExtensions(elt, eventName, detail = {}) {
        let methods = this.#extMethods.get(eventName.replace(/:/g, "_"));
        if (methods) {
          detail.cancelled = false;
          for (const fn of methods) {
            if (fn(elt, detail) === false || detail.cancelled) {
              detail.cancelled = true;
              return false;
            }
          }
        }
        return true;
      }
      timeout(time) {
        time = this.parseInterval(time);
        if (time > 0) {
          return new Promise((resolve) => setTimeout(resolve, time));
        }
      }
      onLoad(callback) {
        this.on(this.#maybeAdjustMetaCharacter("htmx:after:process"), (evt) => {
          callback(evt.target);
        });
      }
      on(eventOrElt, eventOrCallback, callback) {
        let event;
        let elt = document;
        if (callback === void 0) {
          event = eventOrElt;
          callback = eventOrCallback;
        } else {
          elt = this.#normalizeElement(eventOrElt);
          event = eventOrCallback;
        }
        elt.addEventListener(event, callback);
        return callback;
      }
      find(selectorOrElt, selector) {
        return this.#findExt(selectorOrElt, selector);
      }
      findAll(selectorOrElt, selector) {
        return this.#findAllExt(selectorOrElt, selector);
      }
      parseInterval(str) {
        if (typeof str === "number") return str;
        let m = { ms: 1, s: 1e3, m: 6e4 };
        let [, n, u] = str?.match(/^([\d.]+)(ms|s|m)?$/) || [];
        let v = parseFloat(n) * (m[u] || 1);
        return isNaN(v) ? void 0 : v;
      }
      trigger(on2, eventName, detail = {}, bubbles = true) {
        on2 = this.#normalizeElement(on2);
        let evt = new CustomEvent(eventName, {
          detail,
          cancelable: true,
          bubbles,
          composed: true
        });
        let target = on2?.isConnected ? on2 : document;
        let result = !detail.cancelled && target.dispatchEvent(evt);
        return result;
      }
      ajax(verb, path, options) {
        if (!options || options instanceof Element || typeof options === "string") {
          options = { target: options };
        }
        let sourceElt = typeof options.source === "string" ? document.querySelector(options.source) : options.source;
        if (typeof options.source === "string" && !sourceElt) {
          return Promise.reject(new Error("Source not found"));
        }
        if (options.target) {
          let target = this.#resolveTarget(document.body, options.target);
          if (!target) {
            return Promise.reject(new Error("Target not found"));
          }
          sourceElt ||= target;
        }
        sourceElt ||= document.body;
        let ctx = this.#createRequestContext(sourceElt, options.event || {});
        Object.assign(ctx, options);
        if (options.target) ctx.target = this.#resolveTarget(document.body, options.target);
        Object.assign(ctx.request, { action: path, method: verb.toUpperCase() });
        if (options.headers) Object.assign(ctx.request.headers, options.headers);
        return this.#handleTriggerEvent(ctx);
      }
      //============================================================================================
      // History Support
      //============================================================================================
      #pushUrlIntoHistory(path) {
        if (!this.config.history) return;
        if (!history.state) history.replaceState({ htmx: true }, "", location.href);
        history.pushState({ htmx: true }, "", path);
        this.#trigger(document, "htmx:after:history:push", { path });
      }
      #replaceUrlInHistory(path) {
        if (!this.config.history) return;
        history.replaceState({ htmx: true }, "", path);
        this.#trigger(document, "htmx:after:history:replace", { path });
      }
      async #restoreHistory(state, path) {
        await this.timeout(1);
        state ??= history.state;
        if (!state?.htmx) return;
        this.#historyAbort?.abort();
        path = path || location.pathname + location.search;
        let historyElt = document.querySelector(this.#prefixSelector("[hx-history-elt]")) || document.body;
        if (this.#trigger(document, "htmx:before:history:restore", { path, cacheMiss: true })) {
          if (this.config.history === "reload") {
            this._loc.reload();
          } else {
            this.#historyAbort = new AbortController();
            return this.ajax("GET", path, {
              target: historyElt,
              swap: "outerSync",
              select: historyElt !== document.body ? this.#prefixSelector("[hx-history-elt]") : void 0,
              request: {
                headers: { "HX-History-Restore-Request": "true" },
                signal: this.#historyAbort.signal
              }
            });
          }
        }
      }
      #resolveHistoryAction(ctx) {
        let { sourceElement, push, replace, hx, response } = ctx;
        if (hx?.pushurl || hx?.replaceurl) {
          push = hx.pushurl;
          replace = hx.replaceurl;
        }
        if (push == null && replace == null && this.#isBoosted(sourceElement)) {
          push = "true";
        }
        if (push === "false" || push === false) push = null;
        if (replace === "false" || replace === false) replace = null;
        if (!push && !replace) return null;
        let path = push || replace;
        if (path === "true") {
          let finalUrl = response?.raw?.url || ctx.request.action;
          let url = new URL(finalUrl, location.href);
          path = url.pathname + url.search + (ctx.request.anchor ? "#" + ctx.request.anchor : "");
        }
        let type = push ? "push" : "replace";
        return { type, path };
      }
      #handleHistoryUpdate(ctx) {
        if (!this.config.history) return;
        let action = this.#resolveHistoryAction(ctx);
        if (!action) return;
        let historyDetail = {
          history: action,
          sourceElement: ctx.sourceElement,
          response: ctx.response
        };
        if (!this.#trigger(document, "htmx:before:history:update", historyDetail)) return;
        if (action.type === "push") {
          this.#pushUrlIntoHistory(action.path);
        } else {
          this.#replaceUrlInHistory(action.path);
        }
        this.#trigger(document, "htmx:after:history:update", historyDetail);
      }
      // hx-on:<event> binds to <event> directly
      // hx-on::<event> is shorthand for hx-on:htmx:<event> (htmx events)
      #handleHxOnAttributes(node) {
        if (node._htmx?.onInitialized) return;
        let hxOnNames = this.#prefixes("hx-on");
        let mc = this.config.metaCharacter || ":";
        let handler4 = (code) => async (evt) => {
          try {
            await this.#executeJavaScript(
              node,
              { event: evt },
              `with(event?.detail||{}){${code}}`,
              false
            );
          } catch (e) {
            if (typeof e !== "symbol") this.#trigger(node, "htmx:error", { error: e });
          }
        };
        for (let attr of node.getAttributeNames()) {
          let prefix2 = hxOnNames.find((p) => attr.startsWith(p));
          if (!prefix2) continue;
          this.#htmxProp(node).onInitialized = true;
          let rest = attr.substring(prefix2.length);
          let value = node.getAttribute(attr);
          if (!rest) {
            for (let part of value.split(/;(?=[^;]*->)/)) {
              let idx = part.indexOf("->");
              if (idx !== -1) this.#onTrigger(node, part.substring(0, idx).trim(), handler4(part.substring(idx + 2).trim()));
            }
            continue;
          }
          if (rest[0] !== mc) continue;
          let eventName = rest.substring(1);
          if (eventName.startsWith(mc)) eventName = "htmx" + mc + eventName.substring(1);
          this.#onTrigger(node, eventName, handler4(value));
        }
      }
      #showIndicators(elt) {
        let hxIndicator = this.#attributeValue(elt, "hx-indicator");
        let indicatorElements;
        if (!hxIndicator) {
          if (elt === document.body) return [];
          indicatorElements = [elt];
        } else {
          indicatorElements = this.#findAllExt(elt, hxIndicator, "hx-indicator");
        }
        for (const indicator of indicatorElements) {
          let s = this.#htmxState(indicator);
          s.rc = (s.rc || 0) + 1;
          this.#addClass(indicator, this.config.requestClass);
        }
        return indicatorElements;
      }
      #hideIndicators(indicatorElements) {
        for (let indicator of indicatorElements) {
          let s = this.#htmxState(indicator);
          if (s.rc && --s.rc <= 0) {
            this.#removeClass(indicator, this.config.requestClass);
            delete s.rc;
          }
        }
      }
      #disableElements(elt) {
        let hxDisable = this.#attributeValue(elt, "hx-disable");
        let disabledElements = [];
        if (hxDisable) {
          disabledElements = this.#findAllExt(elt, hxDisable, "hx-disable");
          for (let indicator of disabledElements) {
            let s = this.#htmxState(indicator);
            s.dc = (s.dc || 0) + 1;
            indicator.disabled = true;
          }
        }
        return disabledElements;
      }
      #enableElements(disabledElements) {
        for (const indicator of disabledElements) {
          let s = this.#htmxState(indicator);
          if (s.dc && --s.dc <= 0) {
            indicator.disabled = false;
            delete s.dc;
          }
        }
      }
      #collectFormData(elt, form, submitter, validate, isGet) {
        if (validate && form && !form.reportValidity()) return;
        let formData = form ? new FormData(form) : new FormData();
        let included = form ? new Set(form.elements) : /* @__PURE__ */ new Set();
        if (!form) {
          if (validate && elt.reportValidity && !elt.reportValidity()) return;
          this.#addInputValues(elt, included, formData, isGet);
        }
        if (submitter && submitter.name) {
          formData.append(submitter.name, submitter.value);
          included.add(submitter);
        }
        let hxInclude = this.#attributeValue(elt, "hx-include");
        if (hxInclude) {
          for (let node of this.#findAllExt(elt, hxInclude)) {
            if (validate && node.reportValidity && !node.reportValidity()) return;
            this.#addInputValues(node, included, formData);
          }
        }
        return formData;
      }
      #addInputValues(elt, included, formData, isGet) {
        let tag = elt.tagName;
        let inputs = [];
        if (tag === "BUTTON" || tag.includes("-")) {
          inputs = [elt];
        } else if (["INPUT", "SELECT", "TEXTAREA", "FIELDSET"].includes(tag) || !isGet) {
          inputs = this.#queryEltAndDescendants(elt, "[name]:not(button)");
        }
        for (let input of inputs) {
          let name = input.name || input.getAttribute?.("name");
          if (!name || input.matches(":disabled") || included.has(input)) continue;
          included.add(input);
          let type = input.type;
          if (type === "checkbox" || type === "radio" || input.tagName !== "INPUT" && "checked" in input) {
            if (input.checked) {
              formData.append(name, input.value);
            }
          } else if (type === "file") {
            for (let file of input.files) {
              formData.append(name, file);
            }
          } else if (type === "select-multiple") {
            for (let option of input.selectedOptions) {
              formData.append(name, option.value);
            }
          } else if (Array.isArray(input.value)) {
            for (let v of input.value) {
              formData.append(name, v);
            }
          } else {
            formData.append(name, input.value);
          }
        }
      }
      #getAttributeObject(elt, attrName, callback, scope2 = {}) {
        let hxAttr = this.#attributeValue(elt, attrName);
        if (!hxAttr) return null;
        let javascriptContent = this.#extractJavascriptContent(hxAttr);
        if (javascriptContent) {
          if (javascriptContent.indexOf("{") !== 0) {
            javascriptContent = "{" + javascriptContent + "}";
          }
          return this.#executeJavaScript(elt, scope2, javascriptContent, true).then((obj) => {
            callback(obj);
          });
        } else {
          callback(HCON.parse(hxAttr));
        }
      }
      #stringHyperscriptStyleSelector(selector) {
        let s = selector.trim();
        return s.startsWith("<") && s.endsWith("/>") ? s.slice(1, -2) : s;
      }
      #findAllExt(eltOrSelector, maybeSelector, thisAttr, global) {
        let selector = maybeSelector ?? eltOrSelector;
        let elt = maybeSelector ? this.#normalizeElement(eltOrSelector) || document.body : document;
        if (selector.startsWith("global ")) {
          return this.#findAllExt(elt, selector.slice(7), thisAttr, true);
        }
        let parts = selector ? HCON.split(selector) : [];
        let result = [];
        let unprocessedParts = [];
        for (const part of parts) {
          let selector2 = this.#stringHyperscriptStyleSelector(part);
          let item;
          if (selector2.startsWith("closest ")) {
            item = elt.closest(selector2.slice(8));
          } else if (selector2.startsWith("find ")) {
            item = elt.querySelector(selector2.slice(5));
          } else if (selector2.startsWith("findAll ")) {
            result.push(...elt.querySelectorAll(selector2.slice(8)));
          } else if (selector2 === "next" || selector2 === "nextElementSibling") {
            item = elt.nextElementSibling;
          } else if (selector2.startsWith("next ")) {
            item = this.#scanForwardQuery(elt, selector2.slice(5), !!global);
          } else if (selector2 === "previous" || selector2 === "previousElementSibling") {
            item = elt.previousElementSibling;
          } else if (selector2.startsWith("previous ")) {
            item = this.#scanBackwardsQuery(elt, selector2.slice(9), !!global);
          } else if (selector2 === "document") {
            item = document;
          } else if (selector2 === "window") {
            item = window;
          } else if (selector2 === "body") {
            item = document.body;
          } else if (selector2 === "host") {
            item = elt.getRootNode().host;
          } else if (selector2 === "this") {
            if (thisAttr) {
              result.push(...this.#findThisElements(elt, thisAttr));
              continue;
            }
            item = elt;
          } else {
            unprocessedParts.push(selector2);
          }
          if (item) {
            result.push(item);
          }
        }
        if (unprocessedParts.length > 0) {
          let standardSelector = unprocessedParts.join(",");
          let rootNode = this.#getRootNode(elt, !!global);
          result.push(...rootNode.querySelectorAll(standardSelector));
        }
        return [...new Set(result)];
      }
      #scanForwardQuery(start2, match, global) {
        return this.#scanUntilComparison(this.#getRootNode(start2, global).querySelectorAll(match), start2, Node.DOCUMENT_POSITION_PRECEDING);
      }
      #scanBackwardsQuery(start2, match, global) {
        let results = [...this.#getRootNode(start2, global).querySelectorAll(match)].reverse();
        return this.#scanUntilComparison(results, start2, Node.DOCUMENT_POSITION_FOLLOWING);
      }
      #scanUntilComparison(results, start2, comparison) {
        for (const elt of results) {
          if (elt.compareDocumentPosition(start2) === comparison) {
            return elt;
          }
        }
      }
      #getRootNode(elt, global) {
        if (elt.isConnected && elt.getRootNode) {
          return elt.getRootNode?.({ composed: global });
        } else {
          return document;
        }
      }
      #findOrWarn(elt, selector, thisAttr) {
        let result = this.#findAllExt(elt, selector, thisAttr)[0];
        if (!result) {
          console.warn(
            `htmx: '${selector}' on ${thisAttr} did not match any element`,
            { elt, selector, attr: thisAttr }
          );
        }
        return result;
      }
      #findExt(eltOrSelector, selector, thisAttr) {
        return this.#findAllExt(eltOrSelector, selector, thisAttr)[0];
      }
      #extractJavascriptContent(string) {
        if (string != null) {
          if (string.startsWith("js:")) {
            return string.substring(3);
          } else if (string.startsWith("javascript:")) {
            return string.substring(11);
          }
        }
      }
      #initializeAbortListener(elt) {
        let htmxProp = this.#htmxProp(elt);
        if (htmxProp.abortInitialized) return;
        htmxProp.abortInitialized = true;
        let handler4 = () => {
          let requestQueue = this.#getRequestQueue(elt);
          requestQueue.abort();
        };
        elt.addEventListener("htmx:abort", handler4);
        htmxProp.listeners.push({ fromElt: elt, eventName: "htmx:abort", handler: handler4 });
      }
      #morph(oldNode, fragment, innerHTML) {
        let { persistentIds, idMap } = this.#createIdMaps(oldNode, fragment);
        let pantry = document.createElement("div");
        pantry.hidden = true;
        document.body.after(pantry);
        let ctx = { target: oldNode, idMap, persistentIds, pantry, futureMatches: /* @__PURE__ */ new WeakSet() };
        if (innerHTML) {
          this.#morphChildren(ctx, oldNode, fragment);
        } else {
          this.#morphChildren(ctx, oldNode.parentNode, fragment, oldNode, oldNode.nextSibling);
        }
        this.#cleanup(pantry);
        pantry.remove();
      }
      #morphChildren(ctx, oldParent, newParent, insertionPoint = null, endPoint = null) {
        if (oldParent instanceof HTMLTemplateElement && newParent instanceof HTMLTemplateElement) {
          oldParent = oldParent.content;
          newParent = newParent.content;
        }
        insertionPoint ||= oldParent.firstChild;
        let newChild = newParent.firstChild;
        while (newChild) {
          let matchedNode;
          if (insertionPoint && insertionPoint != endPoint) {
            matchedNode = this.#findBestMatch(ctx, newChild, insertionPoint, endPoint);
            if (matchedNode) {
              if (matchedNode !== insertionPoint) {
                let cursor = insertionPoint;
                while (cursor && cursor !== matchedNode) {
                  let tempNode = cursor;
                  cursor = cursor.nextSibling;
                  if (tempNode instanceof Element && (ctx.idMap.has(tempNode) || this.#matchesUpcomingSibling(ctx, tempNode, newChild))) {
                    this.#moveBefore(oldParent, tempNode, endPoint);
                  } else {
                    this.#removeNode(ctx, tempNode);
                  }
                }
              }
            }
          }
          if (!matchedNode && newChild instanceof Element && ctx.persistentIds.has(newChild.id)) {
            let escapedId = CSS.escape(newChild.id);
            matchedNode = ctx.target.id === newChild.id && ctx.target || ctx.target.querySelector(`[id="${escapedId}"]`) || ctx.pantry.querySelector(`[id="${escapedId}"]`);
            let element = matchedNode;
            while (element = element.parentNode) {
              let idSet = ctx.idMap.get(element);
              if (idSet) {
                idSet.delete(matchedNode.id);
                if (!idSet.size) ctx.idMap.delete(element);
              }
            }
            this.#moveBefore(oldParent, matchedNode, insertionPoint);
          }
          if (matchedNode) {
            this.#morphNode(matchedNode, newChild, ctx);
            insertionPoint = matchedNode.nextSibling;
            newChild = newChild.nextSibling;
            continue;
          }
          let nextNewChild = newChild.nextSibling;
          if (ctx.idMap.has(newChild)) {
            let placeholder = document.createElement(newChild.tagName);
            oldParent.insertBefore(placeholder, insertionPoint);
            this.#morphNode(placeholder, newChild, ctx);
            this.process(placeholder);
            insertionPoint = placeholder.nextSibling;
          } else {
            oldParent.insertBefore(newChild, insertionPoint);
            insertionPoint = newChild.nextSibling;
          }
          newChild = nextNewChild;
        }
        while (insertionPoint && insertionPoint != endPoint) {
          let tempNode = insertionPoint;
          insertionPoint = insertionPoint.nextSibling;
          this.#removeNode(ctx, tempNode);
        }
      }
      #matchesUpcomingSibling(ctx, oldElt, startNode) {
        if (ctx.futureMatches.has(oldElt)) return true;
        for (let sibling = startNode.nextSibling, i = 0; sibling && i < this.config.morphScanLimit; sibling = sibling.nextSibling, i++) {
          if (sibling instanceof Element && oldElt.isEqualNode(sibling)) {
            ctx.futureMatches.add(oldElt);
            return true;
          }
        }
        return false;
      }
      #findBestMatch(ctx, node, startPoint, endPoint) {
        if (node.nodeType === 3) return startPoint?.nodeType === 3 ? startPoint : null;
        if (!(node instanceof Element)) return null;
        let softMatch = null, displaceMatchCount = 0, scanLimit = this.config.morphScanLimit;
        let newSet = ctx.idMap.get(node), nodeMatchCount = newSet?.size || 0;
        if (node.id && !newSet) return null;
        let cursor = startPoint;
        while (cursor && cursor != endPoint) {
          let oldSet = ctx.idMap.get(cursor);
          if (this.#internalAPI.isSoftMatch(cursor, node)) {
            if (oldSet && newSet && [...oldSet].some((id) => newSet.has(id))) return cursor;
            if (!oldSet) {
              if (scanLimit > 0 && cursor.isEqualNode(node)) return cursor;
              if (!softMatch) softMatch = cursor;
            }
          }
          displaceMatchCount += oldSet?.size || 0;
          if (displaceMatchCount > nodeMatchCount) break;
          if (document.activeElement?.selectionStart != null && cursor.contains(document.activeElement)) break;
          if (--scanLimit < 1 && nodeMatchCount === 0) break;
          cursor = cursor.nextSibling;
        }
        if (softMatch && this.#matchesUpcomingSibling(ctx, softMatch, node)) return null;
        return softMatch;
      }
      #isSoftMatch(oldNode, newNode) {
        if (!(oldNode instanceof Element) || oldNode.tagName !== newNode.tagName) {
          return false;
        }
        if (oldNode.tagName === "SCRIPT" && !oldNode.isEqualNode(newNode)) return false;
        return !oldNode.id || oldNode.id === newNode.id;
      }
      #removeNode(ctx, node) {
        if (ctx.idMap.has(node)) {
          this.#moveBefore(ctx.pantry, node, null);
        } else {
          this.#cleanup(node);
          node.remove();
        }
      }
      #moveBefore(parentNode, element, after) {
        if (parentNode.moveBefore) {
          try {
            parentNode.moveBefore(element, after);
            return;
          } catch (e) {
          }
        }
        parentNode.insertBefore(element, after);
      }
      #morphNode(oldNode, newNode, ctx) {
        if (oldNode.nodeType === 3) {
          if (oldNode.nodeValue !== newNode.nodeValue) oldNode.nodeValue = newNode.nodeValue;
          return;
        }
        if (this.config.morphSkip && oldNode.matches?.(this.config.morphSkip)) return;
        if (!this.#triggerExtensions(oldNode, "htmx:before:morph:node", { oldNode, newNode })) return;
        this.#copyAttributes(oldNode, newNode);
        if (oldNode instanceof HTMLTextAreaElement && document.activeElement !== oldNode && oldNode.defaultValue != newNode.defaultValue) {
          oldNode.value = newNode.value;
        }
        let skipChildren = this.config.morphSkipChildren && oldNode.matches?.(this.config.morphSkipChildren);
        if (!skipChildren && (!oldNode.isEqualNode(newNode) || newNode.tagName === "TEMPLATE" || newNode.querySelector?.("template"))) {
          this.#morphChildren(ctx, oldNode, newNode);
        }
      }
      #copyAttributes(destination, source) {
        let attributesToIgnore = this.config.morphIgnore || [];
        let needsReinit = false;
        let isHxAttr = (name) => this.#prefixes("hx-").some((p) => name.startsWith(p));
        for (const attr of source.attributes) {
          if (!attributesToIgnore.some((p) => attr.name.startsWith(p)) && destination.getAttribute(attr.name) !== attr.value) {
            if (isHxAttr(attr.name)) needsReinit = true;
            if (!this.#triggerExtensions(destination, "htmx:before:morph:attr", { attrName: attr.name, newValue: attr.value })) continue;
            destination.setAttribute(attr.name, attr.value);
            if (attr.name === "value" && destination instanceof HTMLInputElement && destination.type !== "file" && document.activeElement !== destination) {
              destination.value = attr.value;
            }
          }
        }
        for (let i = destination.attributes.length - 1; i >= 0; i--) {
          let attr = destination.attributes[i];
          if (attr && !source.hasAttribute(attr.name) && !attributesToIgnore.some((p) => attr.name.startsWith(p))) {
            if (isHxAttr(attr.name)) needsReinit = true;
            if (!this.#triggerExtensions(destination, "htmx:before:morph:attr", { attrName: attr.name, newValue: null })) continue;
            destination.removeAttribute(attr.name);
          }
        }
        if (needsReinit) this.#cleanup(destination, true);
      }
      #populateIdMapWithTree(idMap, persistentIds, root, elements) {
        for (const elt of elements) {
          if (persistentIds.has(elt.id)) {
            let current = elt;
            while (current && current !== root) {
              let idSet = idMap.get(current);
              if (idSet == null) {
                idSet = /* @__PURE__ */ new Set();
                idMap.set(current, idSet);
              }
              idSet.add(elt.id);
              current = current.parentElement;
            }
          }
        }
      }
      #createIdMaps(oldNode, newContent) {
        let oldIdElements = this.#queryEltAndDescendants(oldNode, "[id]");
        let newIdElements = newContent.querySelectorAll("[id]");
        let persistentIds = this.#createPersistentIds(oldIdElements, newIdElements);
        let idMap = /* @__PURE__ */ new Map();
        this.#populateIdMapWithTree(idMap, persistentIds, oldNode.parentElement, oldIdElements);
        this.#populateIdMapWithTree(idMap, persistentIds, newContent, newIdElements);
        return { persistentIds, idMap };
      }
      #createPersistentIds(oldIdElements, newIdElements) {
        let duplicateIds = /* @__PURE__ */ new Set(), oldIdTagNameMap = /* @__PURE__ */ new Map();
        for (const { id, tagName } of oldIdElements) {
          if (oldIdTagNameMap.has(id)) duplicateIds.add(id);
          else if (id) oldIdTagNameMap.set(id, tagName);
        }
        let persistentIds = /* @__PURE__ */ new Set();
        for (const { id, tagName } of newIdElements) {
          if (persistentIds.has(id)) duplicateIds.add(id);
          else if (oldIdTagNameMap.get(id) === tagName) persistentIds.add(id);
        }
        for (const id of duplicateIds) persistentIds.delete(id);
        return persistentIds;
      }
      #handleStatusCodes(ctx) {
        let status = ctx.response.raw.status;
        let noSwapStrings = this.config.noSwap.map((x) => x + "");
        let str = status + "";
        for (let pattern of [str, str.slice(0, 2) + "x", str[0] + "xx"]) {
          if (noSwapStrings.includes(pattern)) {
            ctx.swap = "none";
            return;
          }
          let statusValue = this.#attributeValue(ctx.sourceElement, "hx-status:" + pattern);
          if (statusValue) {
            HCON.merge(statusValue, ctx);
            return;
          }
        }
      }
      #submitTransitionTask(task, ctx) {
        return new Promise((resolve) => {
          this.#transitionQueue ||= [];
          this.#transitionQueue.push({ task, resolve, ctx });
          if (!this.#processingTransition) {
            this.#processTransitionQueue();
          }
        });
      }
      async #processTransitionQueue() {
        if (this.#transitionQueue.length === 0 || this.#processingTransition) {
          return;
        }
        this.#processingTransition = true;
        let { task, resolve, ctx } = this.#transitionQueue.shift();
        try {
          if (document.startViewTransition) {
            let detail = { task, ctx };
            this.#trigger(ctx.sourceElement, "htmx:before:viewTransition", detail);
            await document.startViewTransition(detail.task).finished;
            this.#trigger(ctx.sourceElement, "htmx:after:viewTransition", detail);
          } else {
            await task();
          }
        } catch (e) {
        } finally {
          this.#processingTransition = false;
          resolve();
          this.#processTransitionQueue();
        }
      }
      #startCSSTransitions(fragment, root) {
        let idElements = root.querySelectorAll("[id]");
        let existingElementsById = Object.fromEntries([...idElements].map((e) => [e.id, e]));
        let newElementsWithIds = fragment.querySelectorAll("[id]");
        let restoreTasks = [];
        for (let elt of newElementsWithIds) {
          let existing = existingElementsById[elt.id];
          if (existing?.tagName === elt.tagName) {
            let clone2 = elt.cloneNode(false);
            this.#copyAttributes(elt, existing);
            restoreTasks.push(() => {
              this.#copyAttributes(elt, clone2);
            });
          }
        }
        return restoreTasks;
      }
      #addClass(elt, cls) {
        elt?.classList?.add?.(cls);
      }
      #removeClass(elt, cls) {
        elt?.classList?.remove?.(cls);
        if (elt?.classList?.length === 0) elt.removeAttribute("class");
      }
      #normalizeElement(cssOrElement) {
        if (typeof cssOrElement === "string") {
          return this.find(cssOrElement);
        } else {
          return cssOrElement;
        }
      }
      #maybeAdjustMetaCharacter(string) {
        if (this.config.metaCharacter) {
          return string.replace(/:/g, this.config.metaCharacter);
        } else {
          return string;
        }
      }
    }
    return new Htmx();
  })();
  if (typeof window !== "undefined") window.htmx = htmx;

  // node_modules/htmx.org/dist/htmax.min.js
  var htmx2 = (() => {
    const e = { parse(t2) {
      if (!t2) return {};
      if (t2.startsWith("{")) return JSON.parse(t2);
      let r = /(?:"([^"]+)"|'([^']+)'|([^\s,:]+))(?:\s*:\s*(?:"([^"]*)"|'([^']*)'|<((?:[^/]|\/(?!>))+)\/>|([^\s,]+)))?(?=\s|,|$)/g, i = {};
      for (let n of t2.matchAll(r)) {
        let [, t3, r2, s, o, a, l, c] = n, h = t3 ?? r2 ?? s, u = (o ?? a ?? l ?? c ?? "true").trim();
        try {
          u = JSON.parse(u);
        } catch {
        }
        let d = s?.includes("."), f = d ? h.split(".").reduceRight((e2, t4) => ({ [t4]: e2 }), u) : { [h]: u };
        e.merge(f, i);
      }
      return i;
    }, split: (e2) => e2.split(/,(?![^\[]*\])(?![^(]*\))(?![^<]*\/>)(?=(?:[^"']|"[^"]*"|'[^']*')*$)/), merge(t2, r) {
      "string" == typeof t2 && (t2 = e.parse(t2));
      for (let [i, n] of Object.entries(t2)) {
        if (["__proto__", "constructor", "prototype"].includes(i)) continue;
        let t3 = n?.constructor === Object, s = r[i]?.constructor === Object;
        t3 && s ? e.merge(n, r[i]) : r[i] = n;
      }
      return r;
    } };
    class t {
      #e = null;
      #t = [];
      admit(e2, t2, r) {
        if (!this.#e) return this.#e = { strategy: e2, abort: r }, "run";
        if ("replace" === e2 || "abort" !== e2 && "abort" === this.#e.strategy) return this.#t = [], this.#e.abort?.(), this.#e = { strategy: e2, abort: r }, "run";
        if ("queue all" === e2) this.#t.push(t2);
        else if ("queue last" === e2) this.#t = [t2];
        else {
          if ("abort" === e2 || "drop" === e2 || 0 !== this.#t.length) return "dropped";
          this.#t.push(t2);
        }
        return "queued";
      }
      continue() {
        this.#e = null, this.#t.shift()?.();
      }
      abort() {
        this.#e?.abort?.();
      }
    }
    return new class {
      #r = e;
      #i = /* @__PURE__ */ new Map();
      #n = "";
      #s = /* @__PURE__ */ new Set();
      _loc = window.location;
      #o;
      #a = Function;
      #l = Object.getPrototypeOf(async function() {
      }).constructor;
      #c = { createHTML: (e2) => e2, createScript: (e2) => e2 };
      #h;
      #u = "a,form";
      #d = ["get", "post", "put", "patch", "delete", "query"];
      #f;
      #m;
      #p;
      #g;
      #x;
      constructor() {
        this.#b(), this.#y(), this.#h = this.#v("[hx-action],[hx-get],[hx-post],[hx-put],[hx-patch],[hx-delete],[hx-query]"), this.#f = new XPathEvaluator().createExpression(`.//*[@*[${this.#w("hx-on").map((e2) => `starts-with(name(), "${e2}")`).join(" or ")}]]`), this.#o = { HCON: e, attributeValue: this.#S.bind(this), parseTriggerSpecs: this.#E.bind(this), determineMethodAndAction: this.#A.bind(this), createRequestContext: this.#C.bind(this), collectFormData: this.#q.bind(this), getAttributeObject: this.#T.bind(this), insertContent: this.#_.bind(this), morph: this.#O.bind(this), isSoftMatch: this.#N.bind(this), initSecurity: (e2, t3, r) => {
          e2 && (this.#c = e2), t3 && (this.#a = t3), r && (this.#l = r);
        }, onTrigger: this.#H.bind(this), htmxProp: this.#k.bind(this), triggerHtmxEvent: this.#M.bind(this), executeJavaScript: this.#I.bind(this) };
        let t2 = () => this.initialize();
        "loading" === document.readyState ? document.addEventListener("DOMContentLoaded", t2) : setTimeout(t2);
      }
      #b() {
        this.version = "4.0.0", this.config = { logAll: false, prefix: "data-hx-", transitions: false, history: true, mode: "same-origin", defaultSwap: "innerHTML", defaultFocusScroll: false, indicatorClass: "htmx-indicator", requestClass: "htmx-request", includeIndicatorCSS: true, defaultTimeout: 6e4, extensions: "", morphIgnore: ["data-htmx-powered"], morphSkip: "[hx-morph-skip]", morphSkipChildren: "[hx-morph-skip-children]", morphScanLimit: 10, noSwap: [204, 304], implicitInheritance: false, defaultSettleDelay: 1, allowEmptySwapAfterOOB: false };
        let t2 = document.querySelector('meta[name="htmx-config"]');
        t2 && e.merge(t2.content, this.config), this.#n = this.config.extensions;
      }
      #y() {
        if (false !== this.config.includeIndicatorCSS) {
          let e2 = this.config.indicatorClass, t2 = this.config.requestClass, r = new CSSStyleSheet();
          r.replaceSync(`.${e2}{opacity:0;visibility: hidden} .${t2} .${e2}, .${t2}.${e2}{opacity:1;visibility: visible;transition: opacity 200ms ease-in}`), document.adoptedStyleSheets = [...document.adoptedStyleSheets, r];
        }
      }
      registerExtension(e2, t2) {
        return !(this.#n && !this.#n.split(/,\s*/).includes(e2)) && (!this.#s.has(e2) && (this.#s.add(e2), t2.init && t2.init(this.#o), void Object.entries(t2).forEach(([e3, t3]) => {
          this.#i.get(e3)?.push(t3) || this.#i.set(e3, [t3]);
        })));
      }
      #L(e2) {
        let t2 = this.config.prefix;
        return !e2.closest || null != e2.closest("[hx-ignore]") || t2 && null != e2.closest(`[${t2}ignore]`);
      }
      #P(e2, t2) {
        let r = this.config.prefix;
        return e2.getAttribute(t2) ?? (r ? e2.getAttribute(t2.replace("hx-", r)) : null);
      }
      #D(e2, t2) {
        let r = this.config.prefix && t2.replace("hx-", this.config.prefix);
        return e2.hasAttribute(t2) ? t2 : r && e2.hasAttribute(r) ? r : null;
      }
      #v(e2) {
        return this.#w(e2).join(",");
      }
      #w(e2) {
        let t2 = [e2];
        return this.config.prefix && t2.push(e2.replaceAll("hx-", this.config.prefix)), t2;
      }
      #j(e2, t2) {
        let r = [...e2.querySelectorAll?.(t2) ?? []];
        return e2.matches?.(t2) && r.unshift(e2), r;
      }
      #V(e2) {
        return "before" === e2 ? "beforebegin" : "after" === e2 ? "afterend" : "prepend" === e2 ? "afterbegin" : "append" === e2 ? "beforeend" : e2;
      }
      #R(e2, t2) {
        let r = [];
        return this.#S(e2, t2, void 0, (e3, t3) => {
          e3?.split(/\s*[,:]\s*/).includes("this") && r.push(t3);
        }), r;
      }
      #S(e2, t2, r, i) {
        t2 = this.#W(t2);
        let n = this.#W(":inherited"), s = this.#W(":append"), o = this.#P(e2, t2) ?? this.#P(e2, t2 + n);
        if (null != o) return i ? i(o, e2) : o;
        let a = CSS.escape(this.config.implicitInheritance ? t2 : t2 + n), l = CSS.escape(t2 + n + s), c = this.#v(`[${a}],[${l}]`), h = this.#D(e2, t2 + s) ?? this.#D(e2, t2 + n + s);
        if (h) {
          let r2 = e2.getAttribute(h), n2 = e2.parentNode?.closest?.(c);
          if (i && i(r2, e2), n2) {
            let e3 = this.#S(n2, t2, void 0, i);
            return e3 ? (e3 + "," + r2).replace(/[{}]/g, "") : r2;
          }
          return r2;
        }
        let u = e2.parentNode?.closest?.(c);
        return u ? (o = this.#S(u, t2, void 0, i), !i && o && this.config.implicitInheritance && this.#$(e2, "htmx:after:implicitInheritance", { elt: e2, name: t2, parent: u }), o) : r;
      }
      #E(t2) {
        return e.split(t2).flatMap((t3) => {
          let [, r, i] = t3.match(/^\s*(\S+\[[^\]]*\]|\S+)\s*(.*?)\s*$/) ?? [];
          if (!r) return [];
          if (/\[[^\]]*$/.test(r)) throw "unterminated:" + r;
          return [{ name: r, ...e.parse(i) }];
        });
      }
      #A(e2, t2) {
        let r = this.#S(e2, "hx-method"), i = this.#S(e2, "hx-action");
        if (!i) for (let t3 of this.#d) {
          let n = this.#S(e2, "hx-" + t3);
          if (null != n) {
            i = n, r = t3;
            break;
          }
        }
        return this.#B(e2) && (i ||= t2.submitter?.getAttribute?.("formAction") || e2.getAttribute(e2.matches("a") ? "href" : "action")), r ||= t2.submitter?.getAttribute?.("formmethod") || e2.getAttribute("method") || "GET", { action: i, method: r.toUpperCase() };
      }
      #k(e2) {
        return e2._htmx || (e2._htmx = { listeners: [], triggerSpecs: [] }, e2.setAttribute("data-htmx-powered", "true")), e2._htmx;
      }
      #F(e2) {
        return e2._htmx_state ||= {};
      }
      #z(e2) {
        if (this.#U(e2) && this.#M(e2, "htmx:before:init", {}, true)) {
          let t2 = this.#k(e2);
          t2.initialized = true, t2.eventHandler = this.#J(e2), this.#Q(e2), this.#M(e2, "htmx:after:init", {}, true);
        }
      }
      #J(e2) {
        return async (t2) => {
          try {
            let r = this.#C(e2, t2);
            await this.#G(r);
          } catch (t3) {
            this.#M(e2, "htmx:error", { error: t3 });
          }
        };
      }
      #C(t2, r) {
        let { action: i, method: n } = this.#A(t2, r), [s, o] = (i || "").split("#"), a = new AbortController(), l = { sourceElement: t2, sourceEvent: r, status: "created", select: this.#S(t2, "hx-select"), selectOOB: this.#S(t2, "hx-select-oob"), target: this.#S(t2, "hx-target"), swap: this.#S(t2, "hx-swap") ?? this.config.defaultSwap, push: this.#S(t2, "hx-push-url"), replace: this.#S(t2, "hx-replace-url"), transition: this.config.transitions, confirm: this.#S(t2, "hx-confirm"), request: { validate: "true" === this.#S(t2, "hx-validate", !t2.matches("form") || t2.noValidate || r.submitter?.formNoValidate ? "false" : "true"), action: s, anchor: o, method: n, headers: this.#X(t2), abort: a.abort.bind(a), credentials: "same-origin", signal: a.signal, mode: this.config.mode } };
        t2._htmx?.boosted && e.merge(t2._htmx.boosted, l), l.target = this.#K(t2, l.target), l.target && (l.request.headers["HX-Target"] = this.#Y(l.target));
        let c = this.#S(t2, "hx-config");
        return c && (e.merge(c, l.request), l.request.mode = this.config.mode), l;
      }
      #Y(e2) {
        return `${e2.tagName.toLowerCase()}${e2.id ? "#" + encodeURI(e2.id) : ""}`;
      }
      #X(e2) {
        let t2 = { "HX-Request": "true", "HX-Source": this.#Y(e2), "HX-Current-URL": location.href, Accept: "text/html" };
        return this.#B(e2) && (t2["HX-Boosted"] = "true"), t2;
      }
      #Z(e2, t2) {
        return this.#T(e2, "hx-headers", (e3) => {
          for (let r in e3) t2.request.headers[r] = String(e3[r]);
        }, { ctx: t2 });
      }
      #K(e2, t2) {
        return t2 instanceof Element ? t2 : null != t2 ? this.#ee(e2, t2, "hx-target") : this.#B(e2) ? document.body : e2;
      }
      #B(e2) {
        return e2?._htmx?.boosted;
      }
      async #G(e2) {
        let t2 = e2.sourceElement, r = e2.sourceEvent;
        if (!t2.isConnected) return;
        if (this.#te(r)) return;
        this.#re(r) && r.preventDefault();
        let i = /GET|DELETE/.test(e2.request.method), n = i ? t2.matches("form") ? t2 : null : t2.form || t2.closest("form"), s = this.#q(t2, n, r.submitter, e2.request.validate, i);
        if (!s) return;
        let o = this.#T(t2, "hx-vals", (t3) => {
          e2.vals = t3;
          for (let e3 in t3) s.set(e3, t3[e3]);
        }, { ctx: e2 });
        if (o && await o, e2.values) for (let t3 in e2.values) s.delete(t3), s.append(t3, e2.values[t3]);
        let a = this.#Z(t2, e2);
        if (a && await a, Object.assign(e2.request, { form: n, submitter: r.submitter, body: s }), !this.#M(t2, "htmx:config:request", { ctx: e2 })) return;
        if ("DIALOG" === e2.request.method) return;
        let l = this.#ie(e2.request.action);
        if (null != l) {
          let t3 = Object.fromEntries(e2.request.body);
          return void await this.#I(e2.sourceElement, t3, l, false);
        }
        if (i) {
          let t3 = new URL(e2.request.action, document.baseURI);
          for (let r2 of e2.request.body.keys()) t3.searchParams.delete(r2);
          for (let [r2, i2] of e2.request.body) t3.searchParams.append(r2, i2);
          t3.origin === location.origin ? e2.request.action = t3.pathname + t3.search : e2.request.action = t3.href, e2.request.body = null;
        } else "multipart/form-data" !== (this.#S(t2, "hx-encoding") ?? n?.enctype) && (e2.request.body = new URLSearchParams(e2.request.body));
        await this.#ne(e2);
      }
      async #ne(e2) {
        let t2 = e2.sourceElement, r = this.#se(t2), i = this.#oe(t2);
        if (this.#ae(t2), "run" !== i.admit(r, () => this.#ne(e2), () => e2.request?.abort?.())) return;
        e2.status = "issuing";
        let n = [], s = [];
        try {
          if (e2.confirm) {
            if (!await new Promise((r3) => {
              let i2 = { ctx: e2, issueRequest: () => r3(true), dropRequest: () => r3(false) };
              if (this.#M(t2, "htmx:confirm", i2)) {
                let i3 = this.#ie(e2.confirm);
                r3(i3 ? this.#I(t2, { ctx: e2 }, i3, true) : window.confirm(e2.confirm));
              }
            })) return;
          }
          if (this.#le(e2), n = this.#ce(t2), s = this.#he(t2), e2.fetch ||= window.fetch.bind(window), e2.request.headers["HX-Request-Type"] = e2.target === document.body || e2.select ? "full" : "partial", !this.#M(t2, "htmx:before:request", { ctx: e2 })) return;
          let r2 = await e2.fetch(e2.request.action, e2.request);
          if (e2.response = { raw: r2, status: r2.status, headers: r2.headers }, this.#ue(e2), !this.#M(t2, "htmx:before:response", { ctx: e2 })) return;
          if (e2.text = await r2.text(), !this.#M(t2, "htmx:after:request", { ctx: e2 })) return;
          if (e2.response.status >= 400 && this.#M(t2, "htmx:response:error", { ctx: e2 }), this.#de(e2)) return void (e2.keepIndicators = true);
          "issuing" === e2.status && (e2.hx.retarget && (e2.target = e2.hx.retarget), e2.hx.reswap && (e2.swap = e2.hx.reswap), e2.hx.reselect && (e2.select = e2.hx.reselect), e2.status = "response received", this.#fe(e2), await this.swap(e2), e2.status = "swapped");
        } catch (r2) {
          e2.status = "error: " + r2, this.#M(t2, "htmx:error", { ctx: e2, error: r2 });
        } finally {
          await e2.extensionPromise?.catch(() => {
          }), clearTimeout(e2.requestTimeout), e2.hx?.trigger && this.#me(e2.hx.trigger, e2.sourceElement), this.#M(t2, "htmx:finally:request", { ctx: e2 }), e2.keepIndicators || (this.#pe(n), this.#ge(s)), i.continue();
        }
      }
      #ue(e2) {
        e2.hx = {};
        for (let [t2, r] of e2.response.raw.headers) t2.toLowerCase().startsWith("hx-") && (e2.hx[t2.slice(3).toLowerCase().replace(/-/g, "")] = r);
      }
      #de(t2) {
        if ("true" === t2.hx.refresh) return this._loc.reload(), true;
        if (t2.hx.redirect) return this._loc.href = t2.hx.redirect, true;
        if (t2.hx.location) {
          let r = t2.hx.location, i = {}, n = e.parse(r);
          return "{" !== r[0] && null == n.path || (i = n, r = i.path, delete i.path), null == i.push && null == i.replace && (i.push = "true"), this.ajax("GET", r, i), true;
        }
      }
      #le(e2) {
        let t2 = null != e2.request.timeout ? this.parseInterval(e2.request.timeout) : this.config.defaultTimeout;
        t2 && (e2.requestTimeout = setTimeout(() => e2.request?.abort?.(), t2));
      }
      #se(e2) {
        let t2 = this.#S(e2, "hx-sync");
        if (!t2) return "queue first";
        let r = t2.split(":").pop().trim();
        return /^(drop|abort|replace|queue)/.test(r) ? r : "queue first";
      }
      #oe(e2) {
        let r = this.#S(e2, "hx-sync"), i = e2;
        if (r) {
          let t2 = r.includes(":") ? r.slice(0, r.lastIndexOf(":")).trim() : /^(drop|abort|replace|queue)/.test(r) ? null : r;
          t2 && (i = this.#ee(e2, t2, "hx-sync") || e2);
        }
        return this.#F(i).rq ||= new t();
      }
      #te(e2) {
        return "click" === e2.type && (e2.ctrlKey || e2.metaKey || e2.shiftKey) && !!e2.currentTarget?.closest?.("a[href]");
      }
      #re(e2) {
        let t2 = e2.currentTarget;
        if ("submit" === e2.type && "FORM" === t2?.tagName) return true;
        if (!("click" === e2.type && 0 === e2.button)) return false;
        let r = t2?.closest?.('button, input[type="submit"], input[type="image"]'), i = r?.form || r?.closest("form");
        if (r && !r.disabled && i && ("submit" === r.type || "image" === r.type || !r.type && "BUTTON" === r.tagName)) return true;
        let n = t2?.closest?.("a");
        if (!n || !n.href) return false;
        let s = n.getAttribute("href");
        return !(s && s.startsWith("#") && s.length > 1);
      }
      #Q(e2, t2 = e2._htmx.eventHandler) {
        let r = this.#S(e2, "hx-trigger") || (e2.matches("form") ? "submit" : e2.matches("input:not([type=button]):not([type=submit]),select,textarea") ? "change" : "click");
        this.#H(e2, r, t2);
      }
      #H(e2, t2, r) {
        let i = this.#E(t2);
        this.#k(e2).triggerSpecs.push(...i);
        for (let t3 of i) {
          t3.listeners = [];
          let [i2, n] = this.#xe(t3.name), s = [e2];
          "outside" === t3.from ? s = [document] : t3.from && "self" !== t3.from && (s = this.#be(e2, t3.from));
          let o = (e3) => {
            if ((t3.halt || t3.prevent) && e3.preventDefault(), (t3.halt || t3.stop || t3.consume) && e3.stopPropagation(), t3.once) for (let e4 of t3.listeners) e4.fromElt.removeEventListener(e4.eventName, e4.handler, e4);
            r(e3);
          }, a = o;
          if (t3.delay ? a = (e3) => {
            clearTimeout(t3.timeout), t3.timeout = setTimeout(() => o(e3), this.parseInterval(t3.delay));
          } : t3.throttle && (a = (e3) => {
            t3.throttled ? t3.throttledEvent = e3 : (t3.throttled = true, o(e3), t3.throttleTimeout = setTimeout(() => {
              if (t3.throttled = false, t3.throttledEvent) {
                let e4 = t3.throttledEvent;
                t3.throttledEvent = null, a(e4);
              }
            }, this.parseInterval(t3.throttle)));
          }), t3.handler = (r2) => {
            if (("self" !== t3.from || r2.target === e2) && ("outside" !== t3.from || !e2.contains(r2.target)) && (!t3.target || r2.target?.matches?.(t3.target))) {
              if (t3.changed) {
                let e3 = t3.values ??= /* @__PURE__ */ new WeakMap(), r3 = false;
                for (let t4 of s) e3.get(t4) !== t4.value && (r3 = true, e3.set(t4, t4.value));
                if (!r3) return;
              }
              if (n) {
                this.#re(r2) && r2.preventDefault();
                let t4 = {};
                for (let e3 in r2) t4[e3] = r2[e3];
                if (!this.#I(e2, t4, n, true, false)) return;
              }
              a(r2);
            }
          }, "intersect" === i2 || "revealed" === i2) {
            let r2 = { rootMargin: t3.rootMargin };
            t3.root && (r2.root = this.#ee(e2, t3.root)), t3.threshold && (r2.threshold = parseFloat(t3.threshold));
            let n2 = "revealed" === i2;
            t3.observer = new IntersectionObserver((r3) => {
              for (let i3 = 0; i3 < r3.length; i3++) if (r3[i3].isIntersecting) {
                this.trigger(e2, "intersect", {}, false), n2 && t3.observer.disconnect();
                break;
              }
            }, r2), i2 = "intersect", t3.observer.observe(e2);
          }
          if ("every" === i2) {
            let r2 = Object.keys(t3).find((e3) => "name" !== e3);
            t3.interval = setInterval(() => {
              e2.isConnected ? this.#M(e2, "every", {}, false) : clearInterval(t3.interval);
            }, this.parseInterval(r2));
          }
          if ("load" !== i2) for (let r2 of s) {
            let n2 = { fromElt: r2, eventName: i2, handler: t3.handler, capture: !!t3.capture, passive: !!t3.passive };
            e2._htmx.listeners.push(n2), t3.listeners.push(n2), r2.addEventListener(i2, t3.handler, n2);
          }
          else t3.handler(new CustomEvent("load"));
        }
      }
      #xe(e2) {
        let t2 = e2.match(/^([^\[]*)\[([^\]]*)]/);
        return t2 ? [t2[1], t2[2]] : [e2, null];
      }
      #me(t2, r) {
        if ("{" === t2[0]) {
          let i = e.parse(t2);
          for (let e2 in i) {
            let t3 = i[e2], n = r;
            t3?.target && (n = this.find(t3.target)), this.trigger(n, e2, "object" == typeof t3 ? t3 : { value: t3 });
          }
        } else t2.split(",").forEach((e2) => this.trigger(r, e2.trim(), {}));
      }
      #ye(e2) {
        let t2 = {}, r = Object.getPrototypeOf(this);
        for (let i of Object.getOwnPropertyNames(r)) "constructor" !== i && "function" == typeof this[i] && (["find", "findAll"].includes(i) ? t2[i] = (t3, r2) => void 0 === r2 ? this[i](e2, t3) : this[i](t3, r2) : t2[i] = this[i].bind(this));
        return t2;
      }
      #I(e2, t2, r, i = true, n = true, s = false) {
        let o = {};
        Object.assign(o, this.#ye(e2));
        let a = {}, l = { scope: a, code: r };
        this.#$(e2, "htmx:scope", l), r = l.code, Object.assign(o, a), Object.assign(o, t2);
        let c = Object.keys(o), h = Object.values(o), u = new (n ? this.#l : this.#a)(...c, i ? `return (${r})` : r);
        return s ? () => u.call(e2, ...h) : u.call(e2, ...h);
      }
      process(e2, t2) {
        if (!e2?.isConnected) return;
        if (!(e2 instanceof Element)) {
          for (let r2 of e2.children || []) this.process(r2, t2);
          return;
        }
        if (t2 && this.#ve(e2, true), this.#L(e2)) return;
        if (!this.#M(e2, "htmx:before:process")) return;
        let r = [e2], i = this.#f.evaluate(e2), n = null;
        for (; n = i.iterateNext(); ) r.push(n);
        for (let e3 of r) !this.#L(e3) && this.#M(e3, "htmx:before:on:init", {}, true) && this.#we(e3);
        for (let t3 of this.#j(e2, this.#h)) this.#z(t3);
        for (let t3 of this.#j(e2, this.#u)) this.#Se(t3);
        this.#M(e2, "htmx:after:process");
      }
      #Se(e2) {
        let t2 = this.#S(e2, "hx-boost");
        if (t2 && "false" !== t2 && this.#Ee(e2) && this.#M(e2, "htmx:before:init", {}, true)) {
          let r = this.#k(e2);
          r.initialized = true, r.eventHandler = this.#J(e2), r.boosted = t2;
          let i = e2.matches("a") ? "click" : "submit";
          e2._htmx.listeners.push({ fromElt: e2, eventName: i, handler: e2._htmx.eventHandler }), e2.addEventListener(i, e2._htmx.eventHandler), this.#M(e2, "htmx:after:init", {}, true);
        }
      }
      #Ee(e2) {
        if (this.#U(e2)) {
          if ("A" === e2.tagName) {
            if ("" === e2.target || "_self" === e2.target) return !e2.hasAttribute("download") && !e2.getAttribute("href")?.startsWith?.("#") && this.#Ae(e2.href);
          } else if ("FORM" === e2.tagName) return "dialog" !== e2.method && this.#Ae(e2.action);
        }
      }
      #Ae(e2) {
        try {
          return new URL(e2, window.location.href).origin === window.location.origin;
        } catch (e3) {
          return false;
        }
      }
      #U(e2) {
        return !e2._htmx?.initialized && !this.#L(e2);
      }
      #ve(e2, t2) {
        let r = [e2, ...e2.querySelectorAll?.("[data-htmx-powered]") ?? []];
        for (let e3 of r) if (e3._htmx) {
          this.#M(e3, "htmx:before:cleanup");
          for (let t3 of e3._htmx.triggerSpecs || []) t3.interval && clearInterval(t3.interval), t3.timeout && clearTimeout(t3.timeout), t3.throttleTimeout && clearTimeout(t3.throttleTimeout), t3.observer?.disconnect();
          for (let t3 of e3._htmx.listeners || []) t3.fromElt.removeEventListener(t3.eventName, t3.handler, t3);
          e3.removeAttribute("data-htmx-powered"), this.#M(e3, "htmx:after:cleanup"), t2 && delete e3._htmx;
        }
      }
      #Ce(e2) {
        let t2 = document.createElement("div");
        t2.hidden = true, document.body.insertAdjacentElement("afterend", t2);
        let r = e2.querySelectorAll?.(this.#v("[hx-preserve]")) || [];
        for (let e3 of r) {
          let r2 = document.getElementById(e3.id);
          r2 && this.#qe(t2, r2, null);
        }
        return t2;
      }
      #Te(e2) {
        for (let t2 of [...e2.children]) {
          let e3 = document.getElementById(t2.id);
          e3 && (this.#qe(e3.parentNode, t2, e3), this.#ve(e3), e3.remove());
        }
        e2.remove();
      }
      #_e(e2) {
        let t2 = this.#c.createHTML(e2);
        return Document.parseHTMLUnsafe?.(t2) || new DOMParser().parseFromString(t2, "text/html");
      }
      #Oe(e2) {
        let t2 = e2.replace(/<hx-([a-z]+)(\s+|>)/gi, '<template hx type="$1"$2').replace(/<\/hx-[a-z]+>/gi, "</template>"), r = "";
        t2 = t2.replace(/<head(\s[^>]*)?>[\s\S]*?<\/head>/i, (e3) => (r = this.#_e(e3).title, ""));
        let i, n, s = t2.match(/<([a-z][^\/>\x20\t\r\n\f]*)/i)?.[1]?.toLowerCase();
        if ("html" === s || "body" === s ? (i = this.#_e(t2), n = document.createDocumentFragment(), n.append(i.body)) : (i = this.#_e(`<template>${t2}</template>`), n = i.querySelector("template").content), !r) {
          let e3 = n.querySelector("title:not(svg title)");
          e3 && (r = e3.textContent, e3.remove());
        }
        return this.#Ne(n), { fragment: n, title: r };
      }
      #He(e2, t2, r, i) {
        let n = t2.id ? "#" + CSS.escape(t2.id) : null;
        "true" !== r && r && !r.includes(" ") && ([r, n = n] = r.split(/:(.*)/)), "true" !== r && r || (r = "outerHTML");
        let s = this.#ke(r);
        if (n = s.target || n, s.strip ??= !s.style.startsWith("outer"), !n) return;
        let o = [...document.querySelectorAll(n)];
        for (let r2 of o) {
          let n2 = document.createDocumentFragment();
          n2.append(t2.cloneNode(true)), e2.push({ type: "oob", fragment: n2, target: r2, swapSpec: s, sourceElement: i });
        }
        t2.remove();
      }
      #Me(e2, t2, r) {
        let i = [];
        if (r) for (let n of r.split(",")) {
          let [r2, s = "true"] = n.split(/:(.*)/);
          for (let n2 of e2.querySelectorAll(r2)) this.#He(i, n2, s, t2);
        }
        for (let r2 of e2.querySelectorAll(this.#v("[hx-swap-oob]"))) {
          let e3 = this.#D(r2, "hx-swap-oob"), n = r2.getAttribute(e3);
          r2.removeAttribute(e3), this.#He(i, r2, n, t2);
        }
        return i;
      }
      #Ie(e2, t2, r) {
        t2 ? t2.before(...r.childNodes) : e2.append(...r.childNodes);
      }
      #ke(t2) {
        t2 = t2.trim();
        let r = this.config.defaultSwap;
        if (t2 && !/^\S*:/.test(t2)) {
          let e2 = t2.match(/^(\S+)\s*(.*)$/);
          r = e2[1], t2 = e2[2];
        }
        return { style: this.#V(r), ...e.parse(t2) };
      }
      #Le(e2, t2) {
        let r = [];
        for (let i of e2.querySelectorAll("template[hx]")) {
          let e3 = i.getAttribute("type");
          if ("partial" === e3) {
            let e4 = this.#P(i, "hx-target") || (i.id ? "#" + CSS.escape(i.id) : null);
            if (e4) {
              this.#Ne(i.content);
              let n = this.#ke(this.#P(i, "hx-swap") || this.config.defaultSwap), s = this.#be(t2.sourceElement, e4);
              for (let e5 of s.length ? s : [null]) r.push({ type: "partial", fragment: i.content.cloneNode(true), target: e5, swapSpec: n, sourceElement: t2.sourceElement });
            }
          } else this.#$(i, "htmx:process:" + e3, { ctx: t2, tasks: r });
          i.remove();
        }
        return r;
      }
      #Pe(e2, t2, r, i) {
        try {
          null != r && e2.setSelectionRange && e2.setSelectionRange(r, i), e2.focus(t2);
        } catch (e3) {
        }
      }
      #De(e2) {
        let t2 = this.#j(e2, "[autofocus]")[0];
        t2 && this.#Pe(t2);
      }
      #je(e2, t2) {
        if (e2.scroll) {
          let r = e2.scrollTarget ? this.#Ve(e2.scrollTarget) : t2;
          r && ("top" === e2.scroll ? r.scrollTop = 0 : "bottom" === e2.scroll && (r.scrollTop = r.scrollHeight));
        }
        if ("top" === e2.show || "bottom" === e2.show) {
          let r = e2.showTarget ? this.#Ve(e2.showTarget) : t2;
          r?.scrollIntoView?.("top" === e2.show);
        }
      }
      #Re(e2) {
        e2.request?.anchor && document.getElementById(e2.request.anchor)?.scrollIntoView({ block: "start", behavior: "auto" });
      }
      #Ne(e2) {
        let t2 = this.#j(e2, "script");
        for (let e3 of t2) {
          let t3 = document.createElement("script");
          for (let r of e3.attributes) t3.setAttribute(r.name, r.value);
          this.config.inlineScriptNonce && (t3.nonce = this.config.inlineScriptNonce), t3.textContent = this.#c.createScript(e3.textContent), e3.replaceWith(t3);
        }
      }
      initialize() {
        this.config.history && !this.#g && (this.#g = true, history.state || history.replaceState({ htmx: true }, "", location.href), window.navigation && !/firefox/i.test(navigator.userAgent) ? navigation.addEventListener("navigate", (e2) => {
          "traverse" === e2.navigationType && e2.canIntercept && !e2.hashChange && e2.intercept({ handler: () => this.#We() });
        }) : window.addEventListener("popstate", (e2) => this.#We(e2.state))), this.process(document.body);
      }
      async swap(e2) {
        try {
          this.#$e(e2);
          let { fragment: t2, title: r } = this.#Oe(e2.text);
          e2.title = r;
          let i = [], n = this.#Me(t2, e2.sourceElement, e2.selectOOB), s = this.#Le(t2, e2);
          i.push(...n, ...s);
          let o = s.length || n.length && !this.config.allowEmptySwapAfterOOB, a = this.#Be(e2, t2, o);
          if (a && i.unshift(a), !this.#M(e2.sourceElement, "htmx:before:swap", { ctx: e2, tasks: i })) return;
          let l = [], c = [];
          for (let t3 of i) t3.swapSpec?.transition ?? a?.transition ?? e2.transition ? c.push(t3) : l.push(this.#_(t3));
          if (c.length > 0) {
            let t3 = async () => {
              for (let e3 of c) await this.#_(e3, false);
            };
            l.push(this.#Fe(t3, e2));
          }
          await Promise.all(l), !e2.sourceElement?.isConnected && a?.target?.isConnected && (e2.sourceElement = a.target), this.#M(e2.sourceElement, "htmx:after:swap", { ctx: e2 }), e2.title && !a?.swapSpec?.ignoreTitle && (document.title = e2.title), this.#Re(e2);
        } finally {
          this.#M(e2.sourceElement, "htmx:finally:swap", { ctx: e2 });
        }
      }
      #Be(e2, t2, r) {
        let i = this.#ke(e2.swap || this.config.defaultSwap);
        if ("delete" === i.style || t2.childElementCount > 0 || t2.textContent.trim() || (i.swapEmpty ?? !r)) {
          if (e2.select) {
            let r2 = t2.querySelectorAll(e2.select);
            (t2 = document.createDocumentFragment()).append(...r2);
          }
          return this.#B(e2.sourceElement) && (i.show ||= "top"), { type: "main", fragment: t2, target: this.#K(e2.sourceElement || document.body, i.target || e2.target), swapSpec: i, sourceElement: e2.sourceElement, transition: e2.transition && false !== i.transition };
        }
      }
      async #_(e2, t2 = true) {
        let { target: r, swapSpec: i, fragment: n } = e2;
        if ("string" == typeof r && (r = document.querySelector(r)), !r) return;
        "string" == typeof i && (i = this.#ke(i));
        let s, o = i.style;
        if ("none" === o) return;
        if ("BODY" === n.firstElementChild?.tagName) {
          const e3 = r === document.body && o.startsWith("outer");
          e3 && "outerHTML" === o && (o = "outerSync"), i.strip ??= !e3;
        }
        if (i.strip && n.firstElementChild && (n = document.createDocumentFragment(), n.append(...(e2.fragment.firstElementChild.content || e2.fragment.firstElementChild).childNodes)), this.#ze(r, "htmx-swapping"), t2 && e2.swapSpec?.swap && await this.timeout(e2.swapSpec?.swap), "delete" === o) return void (r.parentNode && (this.#ve(r), r.parentNode.removeChild(r)));
        let a = [], l = i.settle ?? this.config.defaultSettleDelay, c = r.parentNode;
        if ("innerHTML" === o || "outerHTML" === o && c) {
          let e3 = document.activeElement;
          if (e3?.id) {
            let t3, r2;
            try {
              t3 = e3.selectionStart, r2 = e3.selectionEnd;
            } catch (e4) {
            }
            s = { elt: e3, start: t3, end: r2 };
          }
          a = t2 && l ? this.#Ue(n, r) : [];
        }
        let h = this.#Ce(n), u = [...n.childNodes];
        try {
          if ("innerHTML" === o) {
            for (const e3 of r.children) this.#ve(e3);
            r.replaceChildren(...n.childNodes);
          } else if ("textContent" === o) {
            for (const e3 of r.querySelectorAll("[data-htmx-powered]")) this.#ve(e3);
            r.textContent = n.textContent;
          } else if ("outerHTML" === o) c && (this.#Ie(c, r, n), this.#ve(r), c.removeChild(r), r = u[0] || c);
          else if ("outerSync" === o) {
            this.#Je(r, n.firstElementChild);
            for (const e3 of r.children) this.#ve(e3);
            r.replaceChildren(...n.firstElementChild.childNodes), u = [r];
          } else if ("innerMorph" === o) this.#O(r, n, true), u = [...r.childNodes];
          else if ("outerMorph" === o) this.#O(r, n, false), u.push(r);
          else if ("beforebegin" === o) c && this.#Ie(c, r, n);
          else if ("afterbegin" === o) this.#Ie(r, r.firstChild, n);
          else if ("beforeend" === o) this.#Ie(r, null, n);
          else if ("afterend" === o) c && this.#Ie(c, r.nextSibling, n);
          else {
            let e3 = this.#i.get("handle_swap") || [], t3 = false;
            for (const s2 of e3) {
              let e4 = s2(o, r, n, i);
              if (e4) {
                t3 = true, Array.isArray(e4) && (u = e4);
                break;
              }
            }
            if (!t3) throw new Error(`Unknown swap style: ${o}`);
          }
        } finally {
          this.#Qe(r, "htmx-swapping");
        }
        if (e2.target = r, this.#Te(h), s && !s.elt.matches(":focus")) {
          let e3 = document.getElementById(s.elt.id);
          if (e3) {
            let t3 = { preventScroll: void 0 !== i.focusScroll ? !i.focusScroll : !this.config.defaultFocusScroll };
            this.#Pe(e3, t3, s.start, s.end);
          }
        }
        this.#M(r, "htmx:before:settle", { task: e2, newContent: u, settleTasks: a });
        for (const e3 of u) this.#ze(e3, "htmx-added");
        if (t2 && a.length > 0) {
          this.#ze(r, "htmx-settling"), await this.timeout(l);
          for (let e3 of a) e3();
          this.#Qe(r, "htmx-settling");
        }
        this.#M(r, "htmx:after:settle", { task: e2, newContent: u, settleTasks: a });
        for (const e3 of u) this.#Qe(e3, "htmx-added"), this.process(e3), this.#De(e3);
        this.#je(i, r);
      }
      #M(e2, t2, r = {}, i = true) {
        if (r.error) {
          let i2 = `htmx: ${t2}: ${r.error.message ?? r.error}`;
          r.error instanceof Error ? console.error(i2, r.error, { elt: e2, detail: r }) : console.error(i2, { elt: e2, detail: r });
        } else r.warn ? console.warn(`htmx: ${t2}: ${r.warn}`, { elt: e2, detail: r }) : this.config.logAll && console.log(`htmx: ${t2}`, { elt: e2, detail: r });
        return e2 = this.#Ge(e2), this.#$(e2, t2, r), this.trigger(e2, this.#W(t2), r, i);
      }
      #$(e2, t2, r = {}) {
        let i = this.#i.get(t2.replace(/:/g, "_"));
        if (i) {
          r.cancelled = false;
          for (const t3 of i) if (false === t3(e2, r) || r.cancelled) return r.cancelled = true, false;
        }
        return true;
      }
      timeout(e2) {
        if ((e2 = this.parseInterval(e2)) > 0) return new Promise((t2) => setTimeout(t2, e2));
      }
      onLoad(e2) {
        this.on(this.#W("htmx:after:process"), (t2) => {
          e2(t2.target);
        });
      }
      on(e2, t2, r) {
        let i, n = document;
        return void 0 === r ? (i = e2, r = t2) : (n = this.#Ge(e2), i = t2), n.addEventListener(i, r), r;
      }
      find(e2, t2) {
        return this.#Ve(e2, t2);
      }
      findAll(e2, t2) {
        return this.#be(e2, t2);
      }
      parseInterval(e2) {
        if ("number" == typeof e2) return e2;
        let [, t2, r] = e2?.match(/^([\d.]+)(ms|s|m)?$/) || [], i = parseFloat(t2) * ({ ms: 1, s: 1e3, m: 6e4 }[r] || 1);
        return isNaN(i) ? void 0 : i;
      }
      trigger(e2, t2, r = {}, i = true) {
        e2 = this.#Ge(e2);
        let n = new CustomEvent(t2, { detail: r, cancelable: true, bubbles: i, composed: true }), s = e2?.isConnected ? e2 : document;
        return !r.cancelled && s.dispatchEvent(n);
      }
      ajax(e2, t2, r) {
        (!r || r instanceof Element || "string" == typeof r) && (r = { target: r });
        let i = "string" == typeof r.source ? document.querySelector(r.source) : r.source;
        if ("string" == typeof r.source && !i) return Promise.reject(new Error("Source not found"));
        if (r.target) {
          let e3 = this.#K(document.body, r.target);
          if (!e3) return Promise.reject(new Error("Target not found"));
          i ||= e3;
        }
        i ||= document.body;
        let n = this.#C(i, r.event || {});
        return Object.assign(n, r), r.target && (n.target = this.#K(document.body, r.target)), Object.assign(n.request, { action: t2, method: e2.toUpperCase() }), r.headers && Object.assign(n.request.headers, r.headers), this.#G(n);
      }
      #Xe(e2) {
        this.config.history && (history.state || history.replaceState({ htmx: true }, "", location.href), history.pushState({ htmx: true }, "", e2), this.#M(document, "htmx:after:history:push", { path: e2 }));
      }
      #Ke(e2) {
        this.config.history && (history.replaceState({ htmx: true }, "", e2), this.#M(document, "htmx:after:history:replace", { path: e2 }));
      }
      async #We(e2, t2) {
        if (await this.timeout(1), e2 ??= history.state, !e2?.htmx) return;
        this.#p?.abort(), t2 = t2 || location.pathname + location.search;
        let r = document.querySelector(this.#v("[hx-history-elt]")) || document.body;
        if (this.#M(document, "htmx:before:history:restore", { path: t2, cacheMiss: true })) {
          if ("reload" !== this.config.history) return this.#p = new AbortController(), this.ajax("GET", t2, { target: r, swap: "outerSync", select: r !== document.body ? this.#v("[hx-history-elt]") : void 0, request: { headers: { "HX-History-Restore-Request": "true" }, signal: this.#p.signal } });
          this._loc.reload();
        }
      }
      #Ye(e2) {
        let { sourceElement: t2, push: r, replace: i, hx: n, response: s } = e2;
        if ((n?.pushurl || n?.replaceurl) && (r = n.pushurl, i = n.replaceurl), null == r && null == i && this.#B(t2) && (r = "true"), "false" !== r && false !== r || (r = null), "false" !== i && false !== i || (i = null), !r && !i) return null;
        let o = r || i;
        if ("true" === o) {
          let t3 = s?.raw?.url || e2.request.action, r2 = new URL(t3, location.href);
          o = r2.pathname + r2.search + (e2.request.anchor ? "#" + e2.request.anchor : "");
        }
        return { type: r ? "push" : "replace", path: o };
      }
      #$e(e2) {
        if (!this.config.history) return;
        let t2 = this.#Ye(e2);
        if (!t2) return;
        let r = { history: t2, sourceElement: e2.sourceElement, response: e2.response };
        this.#M(document, "htmx:before:history:update", r) && ("push" === t2.type ? this.#Xe(t2.path) : this.#Ke(t2.path), this.#M(document, "htmx:after:history:update", r));
      }
      #we(e2) {
        if (e2._htmx?.onInitialized) return;
        let t2 = this.#w("hx-on"), r = this.config.metaCharacter || ":", i = (t3) => async (r2) => {
          try {
            await this.#I(e2, { event: r2 }, `with(event?.detail||{}){${t3}}`, false);
          } catch (t4) {
            "symbol" != typeof t4 && this.#M(e2, "htmx:error", { error: t4 });
          }
        };
        for (let n of e2.getAttributeNames()) {
          let s = t2.find((e3) => n.startsWith(e3));
          if (!s) continue;
          this.#k(e2).onInitialized = true;
          let o = n.substring(s.length), a = e2.getAttribute(n);
          if (!o) {
            for (let t3 of a.split(/;(?=[^;]*->)/)) {
              let r2 = t3.indexOf("->");
              -1 !== r2 && this.#H(e2, t3.substring(0, r2).trim(), i(t3.substring(r2 + 2).trim()));
            }
            continue;
          }
          if (o[0] !== r) continue;
          let l = o.substring(1);
          l.startsWith(r) && (l = "htmx" + r + l.substring(1)), this.#H(e2, l, i(a));
        }
      }
      #ce(e2) {
        let t2, r = this.#S(e2, "hx-indicator");
        if (r) t2 = this.#be(e2, r, "hx-indicator");
        else {
          if (e2 === document.body) return [];
          t2 = [e2];
        }
        for (const e3 of t2) {
          let t3 = this.#F(e3);
          t3.rc = (t3.rc || 0) + 1, this.#ze(e3, this.config.requestClass);
        }
        return t2;
      }
      #pe(e2) {
        for (let t2 of e2) {
          let e3 = this.#F(t2);
          e3.rc && --e3.rc <= 0 && (this.#Qe(t2, this.config.requestClass), delete e3.rc);
        }
      }
      #he(e2) {
        let t2 = this.#S(e2, "hx-disable"), r = [];
        if (t2) {
          r = this.#be(e2, t2, "hx-disable");
          for (let e3 of r) {
            let t3 = this.#F(e3);
            t3.dc = (t3.dc || 0) + 1, e3.disabled = true;
          }
        }
        return r;
      }
      #ge(e2) {
        for (const t2 of e2) {
          let e3 = this.#F(t2);
          e3.dc && --e3.dc <= 0 && (t2.disabled = false, delete e3.dc);
        }
      }
      #q(e2, t2, r, i, n) {
        if (i && t2 && !t2.reportValidity()) return;
        let s = t2 ? new FormData(t2) : new FormData(), o = t2 ? new Set(t2.elements) : /* @__PURE__ */ new Set();
        if (!t2) {
          if (i && e2.reportValidity && !e2.reportValidity()) return;
          this.#Ze(e2, o, s, n);
        }
        r && r.name && (s.append(r.name, r.value), o.add(r));
        let a = this.#S(e2, "hx-include");
        if (a) for (let t3 of this.#be(e2, a)) {
          if (i && t3.reportValidity && !t3.reportValidity()) return;
          this.#Ze(t3, o, s);
        }
        return s;
      }
      #Ze(e2, t2, r, i) {
        let n = e2.tagName, s = [];
        "BUTTON" === n || n.includes("-") ? s = [e2] : !["INPUT", "SELECT", "TEXTAREA", "FIELDSET"].includes(n) && i || (s = this.#j(e2, "[name]:not(button)"));
        for (let e3 of s) {
          let i2 = e3.name || e3.getAttribute?.("name");
          if (!i2 || e3.matches(":disabled") || t2.has(e3)) continue;
          t2.add(e3);
          let n2 = e3.type;
          if ("checkbox" === n2 || "radio" === n2 || "INPUT" !== e3.tagName && "checked" in e3) e3.checked && r.append(i2, e3.value);
          else if ("file" === n2) for (let t3 of e3.files) r.append(i2, t3);
          else if ("select-multiple" === n2) for (let t3 of e3.selectedOptions) r.append(i2, t3.value);
          else if (Array.isArray(e3.value)) for (let t3 of e3.value) r.append(i2, t3);
          else r.append(i2, e3.value);
        }
      }
      #T(t2, r, i, n = {}) {
        let s = this.#S(t2, r);
        if (!s) return null;
        let o = this.#ie(s);
        if (o) return 0 !== o.indexOf("{") && (o = "{" + o + "}"), this.#I(t2, n, o, true).then((e2) => {
          i(e2);
        });
        i(e.parse(s));
      }
      #et(e2) {
        let t2 = e2.trim();
        return t2.startsWith("<") && t2.endsWith("/>") ? t2.slice(1, -2) : t2;
      }
      #be(t2, r, i, n) {
        let s = r ?? t2, o = r ? this.#Ge(t2) || document.body : document;
        if (s.startsWith("global ")) return this.#be(o, s.slice(7), i, true);
        let a = s ? e.split(s) : [], l = [], c = [];
        for (const e2 of a) {
          let t3, r2 = this.#et(e2);
          if (r2.startsWith("closest ")) t3 = o.closest(r2.slice(8));
          else if (r2.startsWith("find ")) t3 = o.querySelector(r2.slice(5));
          else if (r2.startsWith("findAll ")) l.push(...o.querySelectorAll(r2.slice(8)));
          else if ("next" === r2 || "nextElementSibling" === r2) t3 = o.nextElementSibling;
          else if (r2.startsWith("next ")) t3 = this.#tt(o, r2.slice(5), !!n);
          else if ("previous" === r2 || "previousElementSibling" === r2) t3 = o.previousElementSibling;
          else if (r2.startsWith("previous ")) t3 = this.#rt(o, r2.slice(9), !!n);
          else if ("document" === r2) t3 = document;
          else if ("window" === r2) t3 = window;
          else if ("body" === r2) t3 = document.body;
          else if ("host" === r2) t3 = o.getRootNode().host;
          else if ("this" === r2) {
            if (i) {
              l.push(...this.#R(o, i));
              continue;
            }
            t3 = o;
          } else c.push(r2);
          t3 && l.push(t3);
        }
        if (c.length > 0) {
          let e2 = c.join(","), t3 = this.#it(o, !!n);
          l.push(...t3.querySelectorAll(e2));
        }
        return [...new Set(l)];
      }
      #tt(e2, t2, r) {
        return this.#nt(this.#it(e2, r).querySelectorAll(t2), e2, Node.DOCUMENT_POSITION_PRECEDING);
      }
      #rt(e2, t2, r) {
        let i = [...this.#it(e2, r).querySelectorAll(t2)].reverse();
        return this.#nt(i, e2, Node.DOCUMENT_POSITION_FOLLOWING);
      }
      #nt(e2, t2, r) {
        for (const i of e2) if (i.compareDocumentPosition(t2) === r) return i;
      }
      #it(e2, t2) {
        return e2.isConnected && e2.getRootNode ? e2.getRootNode?.({ composed: t2 }) : document;
      }
      #ee(e2, t2, r) {
        let i = this.#be(e2, t2, r)[0];
        return i || console.warn(`htmx: '${t2}' on ${r} did not match any element`, { elt: e2, selector: t2, attr: r }), i;
      }
      #Ve(e2, t2, r) {
        return this.#be(e2, t2, r)[0];
      }
      #ie(e2) {
        if (null != e2) {
          if (e2.startsWith("js:")) return e2.substring(3);
          if (e2.startsWith("javascript:")) return e2.substring(11);
        }
      }
      #ae(e2) {
        let t2 = this.#k(e2);
        if (t2.abortInitialized) return;
        t2.abortInitialized = true;
        let r = () => {
          this.#oe(e2).abort();
        };
        e2.addEventListener("htmx:abort", r), t2.listeners.push({ fromElt: e2, eventName: "htmx:abort", handler: r });
      }
      #O(e2, t2, r) {
        let { persistentIds: i, idMap: n } = this.#st(e2, t2), s = document.createElement("div");
        s.hidden = true, document.body.after(s);
        let o = { target: e2, idMap: n, persistentIds: i, pantry: s, futureMatches: /* @__PURE__ */ new WeakSet() };
        r ? this.#ot(o, e2, t2) : this.#ot(o, e2.parentNode, t2, e2, e2.nextSibling), this.#ve(s), s.remove();
      }
      #ot(e2, t2, r, i = null, n = null) {
        t2 instanceof HTMLTemplateElement && r instanceof HTMLTemplateElement && (t2 = t2.content, r = r.content), i ||= t2.firstChild;
        let s = r.firstChild;
        for (; s; ) {
          let r2;
          if (i && i != n && (r2 = this.#at(e2, s, i, n), r2 && r2 !== i)) {
            let o2 = i;
            for (; o2 && o2 !== r2; ) {
              let r3 = o2;
              o2 = o2.nextSibling, r3 instanceof Element && (e2.idMap.has(r3) || this.#lt(e2, r3, s)) ? this.#qe(t2, r3, n) : this.#ct(e2, r3);
            }
          }
          if (!r2 && s instanceof Element && e2.persistentIds.has(s.id)) {
            let n2 = CSS.escape(s.id);
            r2 = e2.target.id === s.id && e2.target || e2.target.querySelector(`[id="${n2}"]`) || e2.pantry.querySelector(`[id="${n2}"]`);
            let o2 = r2;
            for (; o2 = o2.parentNode; ) {
              let t3 = e2.idMap.get(o2);
              t3 && (t3.delete(r2.id), t3.size || e2.idMap.delete(o2));
            }
            this.#qe(t2, r2, i);
          }
          if (r2) {
            this.#ht(r2, s, e2), i = r2.nextSibling, s = s.nextSibling;
            continue;
          }
          let o = s.nextSibling;
          if (e2.idMap.has(s)) {
            let r3 = document.createElement(s.tagName);
            t2.insertBefore(r3, i), this.#ht(r3, s, e2), this.process(r3), i = r3.nextSibling;
          } else t2.insertBefore(s, i), i = s.nextSibling;
          s = o;
        }
        for (; i && i != n; ) {
          let t3 = i;
          i = i.nextSibling, this.#ct(e2, t3);
        }
      }
      #lt(e2, t2, r) {
        if (e2.futureMatches.has(t2)) return true;
        for (let i = r.nextSibling, n = 0; i && n < this.config.morphScanLimit; i = i.nextSibling, n++) if (i instanceof Element && t2.isEqualNode(i)) return e2.futureMatches.add(t2), true;
        return false;
      }
      #at(e2, t2, r, i) {
        if (3 === t2.nodeType) return 3 === r?.nodeType ? r : null;
        if (!(t2 instanceof Element)) return null;
        let n = null, s = 0, o = this.config.morphScanLimit, a = e2.idMap.get(t2), l = a?.size || 0;
        if (t2.id && !a) return null;
        let c = r;
        for (; c && c != i; ) {
          let r2 = e2.idMap.get(c);
          if (this.#o.isSoftMatch(c, t2)) {
            if (r2 && a && [...r2].some((e3) => a.has(e3))) return c;
            if (!r2) {
              if (o > 0 && c.isEqualNode(t2)) return c;
              n || (n = c);
            }
          }
          if (s += r2?.size || 0, s > l) break;
          if (null != document.activeElement?.selectionStart && c.contains(document.activeElement)) break;
          if (--o < 1 && 0 === l) break;
          c = c.nextSibling;
        }
        return n && this.#lt(e2, n, t2) ? null : n;
      }
      #N(e2, t2) {
        return e2 instanceof Element && e2.tagName === t2.tagName && (!("SCRIPT" === e2.tagName && !e2.isEqualNode(t2)) && (!e2.id || e2.id === t2.id));
      }
      #ct(e2, t2) {
        e2.idMap.has(t2) ? this.#qe(e2.pantry, t2, null) : (this.#ve(t2), t2.remove());
      }
      #qe(e2, t2, r) {
        if (e2.moveBefore) try {
          return void e2.moveBefore(t2, r);
        } catch (e3) {
        }
        e2.insertBefore(t2, r);
      }
      #ht(e2, t2, r) {
        if (3 === e2.nodeType) return void (e2.nodeValue !== t2.nodeValue && (e2.nodeValue = t2.nodeValue));
        if (this.config.morphSkip && e2.matches?.(this.config.morphSkip)) return;
        if (!this.#$(e2, "htmx:before:morph:node", { oldNode: e2, newNode: t2 })) return;
        this.#Je(e2, t2), e2 instanceof HTMLTextAreaElement && document.activeElement !== e2 && e2.defaultValue != t2.defaultValue && (e2.value = t2.value), this.config.morphSkipChildren && e2.matches?.(this.config.morphSkipChildren) || e2.isEqualNode(t2) && "TEMPLATE" !== t2.tagName && !t2.querySelector?.("template") || this.#ot(r, e2, t2);
      }
      #Je(e2, t2) {
        let r = this.config.morphIgnore || [], i = false, n = (e3) => this.#w("hx-").some((t3) => e3.startsWith(t3));
        for (const s of t2.attributes) if (!r.some((e3) => s.name.startsWith(e3)) && e2.getAttribute(s.name) !== s.value) {
          if (n(s.name) && (i = true), !this.#$(e2, "htmx:before:morph:attr", { attrName: s.name, newValue: s.value })) continue;
          e2.setAttribute(s.name, s.value), "value" === s.name && e2 instanceof HTMLInputElement && "file" !== e2.type && document.activeElement !== e2 && (e2.value = s.value);
        }
        for (let s = e2.attributes.length - 1; s >= 0; s--) {
          let o = e2.attributes[s];
          if (o && !t2.hasAttribute(o.name) && !r.some((e3) => o.name.startsWith(e3))) {
            if (n(o.name) && (i = true), !this.#$(e2, "htmx:before:morph:attr", { attrName: o.name, newValue: null })) continue;
            e2.removeAttribute(o.name);
          }
        }
        i && this.#ve(e2, true);
      }
      #ut(e2, t2, r, i) {
        for (const n of i) if (t2.has(n.id)) {
          let t3 = n;
          for (; t3 && t3 !== r; ) {
            let r2 = e2.get(t3);
            null == r2 && (r2 = /* @__PURE__ */ new Set(), e2.set(t3, r2)), r2.add(n.id), t3 = t3.parentElement;
          }
        }
      }
      #st(e2, t2) {
        let r = this.#j(e2, "[id]"), i = t2.querySelectorAll("[id]"), n = this.#dt(r, i), s = /* @__PURE__ */ new Map();
        return this.#ut(s, n, e2.parentElement, r), this.#ut(s, n, t2, i), { persistentIds: n, idMap: s };
      }
      #dt(e2, t2) {
        let r = /* @__PURE__ */ new Set(), i = /* @__PURE__ */ new Map();
        for (const { id: t3, tagName: n2 } of e2) i.has(t3) ? r.add(t3) : t3 && i.set(t3, n2);
        let n = /* @__PURE__ */ new Set();
        for (const { id: e3, tagName: s } of t2) n.has(e3) ? r.add(e3) : i.get(e3) === s && n.add(e3);
        for (const e3 of r) n.delete(e3);
        return n;
      }
      #fe(t2) {
        let r = t2.response.raw.status, i = this.config.noSwap.map((e2) => e2 + ""), n = r + "";
        for (let r2 of [n, n.slice(0, 2) + "x", n[0] + "xx"]) {
          if (i.includes(r2)) return void (t2.swap = "none");
          let n2 = this.#S(t2.sourceElement, "hx-status:" + r2);
          if (n2) return void e.merge(n2, t2);
        }
      }
      #Fe(e2, t2) {
        return new Promise((r) => {
          this.#m ||= [], this.#m.push({ task: e2, resolve: r, ctx: t2 }), this.#x || this.#ft();
        });
      }
      async #ft() {
        if (0 === this.#m.length || this.#x) return;
        this.#x = true;
        let { task: e2, resolve: t2, ctx: r } = this.#m.shift();
        try {
          if (document.startViewTransition) {
            let t3 = { task: e2, ctx: r };
            this.#M(r.sourceElement, "htmx:before:viewTransition", t3), await document.startViewTransition(t3.task).finished, this.#M(r.sourceElement, "htmx:after:viewTransition", t3);
          } else await e2();
        } catch (e3) {
        } finally {
          this.#x = false, t2(), this.#ft();
        }
      }
      #Ue(e2, t2) {
        let r = t2.querySelectorAll("[id]"), i = Object.fromEntries([...r].map((e3) => [e3.id, e3])), n = e2.querySelectorAll("[id]"), s = [];
        for (let e3 of n) {
          let t3 = i[e3.id];
          if (t3?.tagName === e3.tagName) {
            let r2 = e3.cloneNode(false);
            this.#Je(e3, t3), s.push(() => {
              this.#Je(e3, r2);
            });
          }
        }
        return s;
      }
      #ze(e2, t2) {
        e2?.classList?.add?.(t2);
      }
      #Qe(e2, t2) {
        e2?.classList?.remove?.(t2), 0 === e2?.classList?.length && e2.removeAttribute("class");
      }
      #Ge(e2) {
        return "string" == typeof e2 ? this.find(e2) : e2;
      }
      #W(e2) {
        return this.config.metaCharacter ? e2.replace(/:/g, this.config.metaCharacter) : e2;
      }
    }();
  })();
  htmx2.version += "-htmax", htmx2.config.historyCache ??= { disable: true }, (() => {
    let e, t = /* @__PURE__ */ new Set();
    function r(e2) {
      for (let t2 of Object.keys(e2)) "last-event-id" === t2.toLowerCase() && delete e2[t2];
    }
    async function* i(e2) {
      let t2 = e2.reader, r2 = e2.lastEventId, i2 = new TextDecoder(), n2 = "", s2 = false, o = false, a = { data: "", event: "", retry: null }, l = true;
      try {
        for (; ; ) {
          let { done: e3, value: c } = await t2.read();
          if (e3) break;
          let h = i2.decode(c, { stream: true });
          l && (65279 === h.charCodeAt(0) && (h = h.slice(1)), l = false), n2 += h;
          let u = n2.split(/\r\n|\r|\n/);
          n2 = u.pop() || "";
          for (let e4 of u) {
            if (!e4) {
              (s2 || o || a.event) && (yield { ...a, id: r2, hasData: s2, hasId: o }), s2 = false, o = false, a = { data: "", event: "", retry: null };
              continue;
            }
            let t3, i3, n3 = e4.indexOf(":");
            if (0 !== n3) {
              if (n3 < 0 ? (t3 = e4, i3 = "") : (t3 = e4.slice(0, n3), i3 = e4.slice(n3 + 1), " " === i3[0] && (i3 = i3.slice(1))), "data" === t3) a.data += (s2 ? "\n" : "") + i3, s2 = true;
              else if ("event" === t3) a.event = i3;
              else if ("id" === t3) i3.includes("\0") || (r2 = i3, o = true);
              else if ("retry" === t3) {
                let e5 = parseInt(i3, 10);
                isNaN(e5) || (a.retry = e5);
              }
            }
          }
        }
      } finally {
        t2.releaseLock(), e2.reader = null;
      }
    }
    async function n(t2, n2) {
      let o = t2.sourceElement, a = (function(t3) {
        let r2 = null != e.attributeValue(t3, "hx-sse:connect"), i2 = e.HCON.parse(e.attributeValue(t3, "hx-config")).sse || {};
        return { reconnect: r2, reconnectDelay: 500, reconnectMaxDelay: 6e4, reconnectMaxAttempts: 1 / 0, reconnectJitter: 0.3, pauseOnBackground: r2, releaseOn: r2 ? "immediate" : "end", ...htmx2.config.sse, ...i2 };
      })(o), l = false;
      function c() {
        n2 && (n2(), n2 = null);
      }
      "immediate" === a.releaseOn && c();
      let h = { url: t2.request.action, config: a, abortController: { abort: t2.request.abort, signal: t2.request.signal }, reader: null, lastEventId: "", delayCanceller: null, visibilityHandler: null, attempt: 0, cancelled: false, status: null };
      e.htmxProp(o).sse = h;
      let u = false, d = null;
      if (a.pauseOnBackground) {
        let e2 = () => {
          document.hidden ? (u = true, h.reader?.cancel()) : u && (u = false, d && d());
        };
        document.addEventListener("visibilitychange", e2), h.visibilityHandler = e2;
      }
      if (h.cancelled = false, !e.triggerHtmxEvent(o, "htmx:sse:before:connection", { connection: h }) || h.cancelled) return void s(o, "cancelled");
      h.status = t2.response.status, e.triggerHtmxEvent(o, "htmx:sse:after:connection", { connection: h });
      let f = t2.response.raw;
      try {
        for (; o.isConnected; ) {
          if (h.attempt > 0) {
            if (u) {
              if (await new Promise((e2) => {
                d = e2;
              }), d = null, !o.isConnected) break;
              h.attempt = 1, l = true;
            }
            if (!l && (!a.reconnect || h.attempt > a.reconnectMaxAttempts)) break;
            let i2 = htmx2.parseInterval(a.reconnectDelay) ?? a.reconnectDelay, n3 = htmx2.parseInterval(a.reconnectMaxDelay) ?? a.reconnectMaxDelay, s2 = Math.min(i2 * Math.pow(2, h.attempt - 1), n3);
            if (a.reconnectJitter > 0) {
              let e2 = s2 * a.reconnectJitter;
              s2 = Math.max(0, s2 + (2 * Math.random() - 1) * e2);
            }
            if (h.cancelled = false, !e.triggerHtmxEvent(o, "htmx:sse:before:connection", { connection: h }) || h.cancelled) break;
            if (await new Promise((e2) => {
              h.delayCanceller = e2, setTimeout(e2, s2);
            }), h.delayCanceller = null, !o.isConnected) break;
            let c2 = new AbortController();
            h.abortController = c2;
            try {
              r(t2.request.headers), h.lastEventId && (t2.request.headers["Last-Event-ID"] = h.lastEventId), f = await fetch(t2.request.action, { ...t2.request, signal: c2.signal });
            } catch (t3) {
              if (c2.signal.aborted) break;
              e.triggerHtmxEvent(o, "htmx:sse:error", { connection: h, error: t3 }), l = false, h.attempt++;
              continue;
            }
            if (!f.ok) {
              e.triggerHtmxEvent(o, "htmx:sse:error", { connection: h, error: new Error(`SSE reconnect failed with status ${f.status}`), status: f.status }), l = false, h.attempt++;
              continue;
            }
            h.status = f.status, e.triggerHtmxEvent(o, "htmx:sse:after:connection", { connection: h }), h.attempt = 0;
          }
          l = false;
          try {
            h.reader = f.body.getReader();
            for await (let n3 of i(h)) {
              if (!o.isConnected || l) break;
              if (n3.hasId && (h.lastEventId = n3.id, n3.id || r(t2.request.headers)), !n3.hasData && !n3.event) continue;
              let i2 = [], u2 = { connection: h, message: { data: n3.data, event: n3.event, id: n3.id }, cancelled: false, waitUntil: (e2) => i2.push(Promise.resolve(e2)) }, d2 = e.triggerHtmxEvent(o, "htmx:sse:before:message", u2);
              if (await Promise.all(i2), d2 && !u2.cancelled) {
                if (null != n3.retry && (a.reconnectDelay = n3.retry), u2.message.event) {
                  "hx:release" === u2.message.event && c(), htmx2.trigger(o, u2.message.event, { data: u2.message.data, id: u2.message.id }), e.triggerHtmxEvent(o, "htmx:sse:after:message", { connection: h, message: u2.message });
                  let t3 = e.attributeValue(o, "hx-sse:close");
                  if (t3 && u2.message.event === t3) return void s(o, "message");
                  continue;
                }
                t2.text = u2.message.data, t2.swap.includes("swapEmpty") || (t2.swap += " swapEmpty:false"), await htmx2.swap(t2), "first" === a.releaseOn && c(), e.triggerHtmxEvent(o, "htmx:sse:after:message", { connection: h, message: u2.message });
              }
            }
          } catch (t3) {
            h.abortController?.signal?.aborted || "AbortError" === t3.name || e.triggerHtmxEvent(o, "htmx:sse:error", { connection: h, error: t3 });
          }
          if (!o.isConnected) break;
          h.attempt++;
        }
      } finally {
        c(), s(o, o.isConnected ? "ended" : "removed");
      }
    }
    function s(t2, r2) {
      let i2 = t2?._htmx?.sse;
      i2 && (i2.abortController?.abort(), i2.reader?.cancel?.(), i2.delayCanceller && i2.delayCanceller(), i2.visibilityHandler && document.removeEventListener("visibilitychange", i2.visibilityHandler), e.triggerHtmxEvent(t2, "htmx:sse:close", { connection: i2, reason: r2 || "cleanup" }), delete t2._htmx.sse);
    }
    htmx2.registerExtension("sse", { init: (t2) => {
      e = t2;
    }, htmx_config_request: (e2, { ctx: { request: t2 } }) => {
      t2.headers.Accept = `${t2.headers.Accept ?? t2.headers.accept ?? "text/html"}, text/event-stream`;
    }, htmx_before_response: (t2, r2) => {
      let i2, s2 = r2.ctx, o = s2.response.raw.headers.get("Content-Type");
      if (o?.includes("text/event-stream")) return clearTimeout(s2.requestTimeout), s2.extensionPromise = new Promise((e2) => i2 = e2), n(s2, i2).catch((r3) => {
        "AbortError" !== r3.name && e.triggerHtmxEvent(t2, "htmx:sse:error", { error: r3, url: s2.request.action });
      }), false;
    }, htmx_after_process: (r2) => {
      let i2 = htmx2.config.metaCharacter || ":", n2 = (r3) => {
        !(function(e2) {
          for (let r4 of ["sse-connect", "sse-close", "sse-swap"]) e2.hasAttribute(r4) && !t.has(r4) && ("sse-swap" === r4 ? console.warn("htmx: [hx-sse] sse-swap is removed in htmx 4. Unnamed SSE messages are swapped automatically. Named events are dispatched as DOM events.") : console.warn(`htmx: [hx-sse] legacy attribute ${r4} is deprecated; use hx-sse:${r4.slice(4)} instead`), t.add(r4));
        })(r3);
        for (let e2 of ["connect", "close"]) {
          let t2 = `sse-${e2}`;
          if (!r3.hasAttribute(t2)) continue;
          let n3 = (htmx2.config.prefix || "hx-") + "sse" + i2 + e2;
          r3.hasAttribute(n3) || r3.setAttribute(n3, r3.getAttribute(t2));
        }
        !(function(t2) {
          let r4 = e.attributeValue(t2, "hx-sse:connect");
          if (!r4) return;
          if (t2._htmx?.sse) return;
          let i3 = e.attributeValue(t2, "hx-trigger") || "load";
          e.onTrigger(t2, i3, () => {
            t2._htmx?.sse || htmx2.ajax("GET", r4, { source: t2 });
          });
        })(r3);
      };
      n2(r2);
      let s2 = `[${CSS.escape("hx-sse" + i2 + "connect")}]`;
      htmx2.config.prefix && (s2 += `,[${CSS.escape(htmx2.config.prefix + "sse" + i2 + "connect")}]`), r2.querySelectorAll(`${s2},[sse-connect],[sse-close],[sse-swap]`).forEach(n2);
    }, htmx_before_cleanup: (e2) => {
      s(e2);
    } });
  })(), (() => {
    let e;
    function t(e2) {
      let t2 = htmx2.config.metaCharacter || ":", r2 = `[${CSS.escape("hx-ws" + t2 + e2)}]`;
      return htmx2.config.prefix && (r2 += `,[${CSS.escape(htmx2.config.prefix + "ws" + t2 + e2)}]`), r2;
    }
    function r(t2) {
      let r2 = e.HCON.parse(e.attributeValue(t2, "hx-config")).ws || {};
      return { reconnect: true, reconnectCodes: [1001, 1005, 1006, 1011, 1012, 1013, 1014], reconnectDelay: 500, reconnectMaxDelay: 6e4, reconnectMaxAttempts: 1 / 0, reconnectJitter: 0.3, pauseOnBackground: true, maxOutgoingMessagesQueueSize: 100, ...htmx2.config.ws, ...r2 };
    }
    function i(e2) {
      if (e2.startsWith("ws://") || e2.startsWith("wss://")) return e2;
      if (e2.startsWith("http://")) return "ws://" + e2.slice(7);
      if (e2.startsWith("https://")) return "wss://" + e2.slice(8);
      let t2 = "https:" === window.location.protocol ? "wss:" : "ws:", r2 = window.location.host;
      return e2.startsWith("//") ? t2 + e2 : e2.startsWith("/") ? t2 + "//" + r2 + e2 : t2 + "//" + r2 + window.location.pathname.substring(0, window.location.pathname.lastIndexOf("/") + 1) + e2;
    }
    const n = /* @__PURE__ */ new Set();
    function s(e2, t2) {
      if (t2.timer && clearTimeout(t2.timer), t2.visibilityHandler && document.removeEventListener("visibilitychange", t2.visibilityHandler), t2.abortController && t2.abortController.abort(), t2.queue.length = 0, t2.socket) try {
        t2.socket.readyState !== WebSocket.OPEN && t2.socket.readyState !== WebSocket.CONNECTING || t2.socket.close();
      } catch (e3) {
      }
      e2._htmx?.ws?.connection === t2 && delete e2._htmx.ws.connection, n.delete(t2);
    }
    function o(t2, r2) {
      let i2 = r2.url;
      if (r2.abortController && r2.abortController.abort(), r2.socket) {
        let e2 = r2.socket;
        r2.socket = null;
        try {
          e2.readyState !== WebSocket.OPEN && e2.readyState !== WebSocket.CONNECTING || e2.close();
        } catch (e3) {
        }
      }
      try {
        r2.socket = new WebSocket(i2, r2.config?.protocols);
        let l2 = new AbortController();
        r2.abortController = l2;
        let c2 = { signal: l2.signal };
        r2.socket.addEventListener("open", () => {
          t2.isConnected ? (e.triggerHtmxEvent(t2, "htmx:ws:after:connection", { connection: r2 }), r2.attempt = 0, (function(e2) {
            for (; e2.queue.length && e2.socket?.readyState === WebSocket.OPEN; ) {
              let t3 = e2.queue.shift();
              a(e2, t3.element, t3.message);
            }
          })(r2)) : s(t2, r2);
        }, c2), r2.socket.addEventListener("message", (i3) => {
          r2.receiving = r2.receiving.then(() => (async function(t3, r3, i4) {
            let n2 = [], o2 = { data: i4.data, text() {
              let e2 = o2.data;
              return "string" == typeof e2 ? Promise.resolve(e2) : e2 instanceof Blob ? e2.text() : Promise.resolve(new TextDecoder().decode(e2));
            }, json: () => o2.text().then(JSON.parse) };
            if (!t3.isConnected) return void s(t3, r3);
            let a2 = { connection: r3, message: o2, cancelled: false, waitUntil(e2) {
              n2.push(Promise.resolve(e2));
            } }, l3 = e.triggerHtmxEvent(t3, "htmx:ws:before:message:incoming", a2);
            if (await Promise.all(n2), !l3 || a2.cancelled) return;
            let c3, h = null;
            if ("string" == typeof o2.data) try {
              h = await o2.json();
            } catch (e2) {
              c3 = await o2.text();
            }
            h && (void 0 !== h.content ? c3 = h.content : void 0 !== h.payload && (c3 = h.payload, r3._payloadWarnFired || (console.warn("htmx: [hx-ws] json.payload is deprecated; use json.content instead"), r3._payloadWarnFired = true)));
            if (null != c3) {
              let r4 = h?.target || e.attributeValue(t3, "hx-target"), i5 = h?.swap || e.attributeValue(t3, "hx-swap") || htmx2.config.defaultSwap;
              /(?:^|\s)swapEmpty(?::(?:true|false))?(?=\s|$)/.test(i5) || (i5 += " swapEmpty:false"), await htmx2.swap({ sourceElement: t3, target: r4 || t3, swap: i5, select: h?.select ?? e.attributeValue(t3, "hx-select"), selectOOB: e.attributeValue(t3, "hx-select-oob"), text: c3, transition: false });
            }
            e.triggerHtmxEvent(t3, "htmx:ws:after:message:incoming", { connection: r3, message: o2 });
          })(t2, r2, i3)).catch((i4) => {
            t2.isConnected && e.triggerHtmxEvent(t2, "htmx:ws:error", { connection: r2, error: i4 });
          });
        }, c2), r2.socket.addEventListener("close", (i3) => {
          if (i3.target !== r2.socket) return;
          if (t2.isConnected && e.triggerHtmxEvent(t2, "htmx:ws:close", { connection: r2, reason: "closed", code: i3.code }), !n.has(r2)) return;
          let a2 = r2.config;
          a2.pauseOnBackground && document.hidden || (a2.reconnect && a2.reconnectCodes.includes(i3.code) && t2.isConnected ? (function(t3, r3) {
            let i4 = r3.config;
            r3.attempt++;
            let n2 = r3.attempt;
            if (!i4.reconnect || n2 > i4.reconnectMaxAttempts) return void s(t3, r3);
            let a3 = htmx2.parseInterval(i4.reconnectDelay) ?? i4.reconnectDelay, l3 = htmx2.parseInterval(i4.reconnectMaxDelay) ?? i4.reconnectMaxDelay, c3 = Math.min(a3 * Math.pow(2, n2 - 1), l3);
            if (i4.reconnectJitter > 0) {
              let e2 = c3 * i4.reconnectJitter;
              c3 = Math.max(0, c3 + (2 * Math.random() - 1) * e2);
            }
            if (!t3.isConnected) return void s(t3, r3);
            if (r3.cancelled = false, !e.triggerHtmxEvent(t3, "htmx:ws:before:connection", { connection: r3 }) || r3.cancelled) return e.triggerHtmxEvent(t3, "htmx:ws:close", { connection: r3, reason: "cancelled", code: null }), void s(t3, r3);
            r3.timer = setTimeout(() => {
              t3.isConnected ? o(t3, r3) : s(t3, r3);
            }, c3);
          })(t2, r2) : s(t2, r2));
        }, c2), r2.socket.addEventListener("error", (i3) => {
          t2.isConnected && e.triggerHtmxEvent(t2, "htmx:ws:error", { connection: r2, error: i3 });
        }, c2);
      } catch (i3) {
        t2.isConnected && e.triggerHtmxEvent(t2, "htmx:ws:error", { connection: r2, error: i3 });
      }
    }
    function a(t2, r2, i2) {
      try {
        t2.socket.send(i2.data), e.triggerHtmxEvent(r2, "htmx:ws:after:message:outgoing", { connection: t2, message: i2 });
      } catch (i3) {
        e.triggerHtmxEvent(r2, "htmx:ws:error", { connection: t2, error: i3 });
      }
    }
    function l(t2) {
      if (e.htmxProp(t2).ws ??= {}, t2._htmx.ws.initialized) return;
      let s2 = e.attributeValue(t2, "hx-ws:connect");
      if (!s2) return;
      let a2 = e.attributeValue(t2, "hx-trigger") || "load";
      e.onTrigger(t2, a2, () => {
        !(function(t3, s3) {
          if (s3._htmx.ws.connection) return s3._htmx.ws.connection;
          let a3 = { url: i(t3), config: r(s3), socket: null, attempt: 0, timer: null, queue: [], receiving: Promise.resolve(), sending: Promise.resolve(), abortController: null, visibilityHandler: null, cancelled: false };
          !e.triggerHtmxEvent(s3, "htmx:ws:before:connection", { connection: a3 }) || a3.cancelled ? e.triggerHtmxEvent(s3, "htmx:ws:close", { connection: a3, reason: "cancelled", code: null }) : (s3._htmx.ws.connection = a3, n.add(a3), o(s3, a3), a3.config.pauseOnBackground && (a3.visibilityHandler = () => {
            document.hidden ? a3.socket && a3.socket.readyState === WebSocket.OPEN && a3.socket.close() : a3.socket && a3.socket.readyState !== WebSocket.CLOSED || (a3.attempt = 0, o(s3, a3));
          }, document.addEventListener("visibilitychange", a3.visibilityHandler)));
        })(s2, t2);
      }), t2._htmx.ws.initialized = true;
    }
    function c(r2) {
      if (e.htmxProp(r2).ws ??= {}, r2._htmx.ws.sendInitialized) return;
      let s2 = e.attributeValue(r2, "hx-trigger");
      s2 || (s2 = r2.matches("form") ? "submit" : r2.matches("input:not([type=button]):not([type=submit]),select,textarea") ? "change" : "click"), e.onTrigger(r2, s2, async (s3) => {
        r2.matches("form") && "submit" === s3.type && s3.preventDefault(), await (async function(r3, s4) {
          let o2 = r3.closest(t("connect")), l2 = o2 && e.attributeValue(o2, "hx-ws:connect");
          if (!l2) return void e.triggerHtmxEvent(r3, "htmx:ws:error", { url: null, error: "No WebSocket connection found for element" });
          let c2 = i(l2), h = o2._htmx?.ws?.connection;
          if (!h) return void e.triggerHtmxEvent(r3, "htmx:ws:error", { url: c2, error: "Connection not open" });
          let u = { ...e.createRequestContext(r3, s4).request.headers };
          delete u.Accept;
          let d = r3.form || r3.closest("form"), f = e.collectFormData(r3, d, s4.submitter), m = {};
          for (let [e2, t2] of f) m[e2] = e2 in m ? [].concat(m[e2], t2) : t2;
          let p = e.getAttributeObject(r3, "hx-vals", (e2) => Object.assign(m, e2)), g = h.sending.then(async () => {
            p && await p, delete m.headers;
            let t2 = [], i2 = { headers: u, values: m, data: void 0 }, s5 = { connection: h, message: i2, cancelled: false, waitUntil(e2) {
              t2.push(Promise.resolve(e2));
            } }, o3 = e.triggerHtmxEvent(r3, "htmx:ws:before:message:outgoing", s5);
            try {
              if (await Promise.all(t2), !o3 || s5.cancelled) return;
              if (i2.data ??= JSON.stringify({ ...i2.values, headers: i2.headers }), !n.has(h)) return void e.triggerHtmxEvent(r3, "htmx:ws:error", { connection: h, error: "Connection closed" });
              h.socket?.readyState === WebSocket.OPEN ? a(h, r3, i2) : h.queue.length >= h.config.maxOutgoingMessagesQueueSize ? e.triggerHtmxEvent(r3, "htmx:ws:error", { connection: h, error: "Outgoing messages queue is full" }) : h.queue.push({ element: r3, message: i2 });
            } catch (t3) {
              e.triggerHtmxEvent(r3, "htmx:ws:error", { connection: h, error: t3 });
            }
          });
          h.sending = g.catch(() => {
          }), await g;
        })(r2, s3);
      }), r2._htmx.ws.sendInitialized = true;
    }
    htmx2.registerExtension("ws", { init: (t2) => {
      e = t2, htmx2.config.ws || (htmx2.config.ws = {});
    }, htmx_after_process: (r2) => {
      const i2 = (t2) => {
        !(function(e2) {
          if (e2.hasAttribute("ws-connect") || e2.hasAttribute("ws-send")) {
            if (console.warn("htmx: [hx-ws] legacy attributes ws-connect and ws-send are deprecated; use hx-ws:connect and hx-ws:send instead"), e2.hasAttribute("ws-connect")) {
              let t3 = e2.getAttribute("ws-connect"), r4 = htmx2.config.metaCharacter || ":", i3 = (htmx2.config.prefix || "hx-") + "ws" + r4 + "connect";
              e2.hasAttribute(i3) || e2.setAttribute(i3, t3);
            }
            if (e2.hasAttribute("ws-send")) {
              let t3 = htmx2.config.metaCharacter || ":", r4 = (htmx2.config.prefix || "hx-") + "ws" + t3 + "send";
              e2.hasAttribute(r4) || e2.setAttribute(r4, "");
            }
          }
        })(t2);
        let r3 = e.attributeValue(t2, "hx-ws:send");
        null != e.attributeValue(t2, "hx-ws:connect") && l(t2), null != r3 && c(t2);
      };
      i2(r2);
      let n2 = t("connect") + "," + t("send") + ",[ws-connect],[ws-send]";
      r2.querySelectorAll(n2).forEach(i2);
    }, htmx_before_cleanup: (t2) => {
      !(function(t3) {
        let r2 = t3._htmx?.ws?.connection;
        r2 && (e.triggerHtmxEvent(t3, "htmx:ws:close", { connection: r2, reason: "removed", code: null }), s(t3, r2));
      })(t2);
    } }), "undefined" != typeof window && window.htmx && window.addEventListener("pagehide", () => {
      n.forEach((e2) => {
        e2.socket && e2.socket.close(1e3, "page navigating away");
      });
    });
  })(), (() => {
    let e;
    htmx2.registerExtension("preload", { init: (t) => {
      e = t;
    }, htmx_after_init: (t) => {
      !(function(t2) {
        let r = e.attributeValue(t2, "hx-preload");
        if (null == r && !t2._htmx?.boosted) return;
        let i = [], n = 5e3;
        if (r) {
          let t3 = e.parseTriggerSpecs(r);
          if (0 === t3.length) return;
          for (const e2 of t3) i.push(e2.name), e2.timeout && (n = htmx2.parseInterval(e2.timeout));
        } else {
          let r2 = t2._htmx?.boosted && "A" === t2.tagName, s2 = null != e.attributeValue(t2, "hx-get");
          if (!r2 && !s2) return;
          if (r2 && false === htmx2.config?.preload?.autoBoost) return;
          htmx2.config?.preload?.boostTimeout && (n = htmx2.parseInterval(htmx2.config.preload.boostTimeout)), i.push(htmx2.config?.preload?.boostEvent || "mousedown"), i.push("touchstart");
        }
        let s = async (r2) => {
          let { method: i2 } = e.determineMethodAndAction(t2, r2);
          if ("GET" !== i2) return;
          if (t2._htmx?.preload) return;
          let s2 = e.createRequestContext(t2, r2), o = t2.form || t2.closest("form"), a = e.collectFormData(t2, o, r2.submitter), l = e.getAttributeObject(t2, "hx-vals", (e2) => {
            for (let t3 in e2) a.set(t3, e2[t3]);
          });
          l && await l;
          let c = s2.request.action.replace?.(/#.*$/, ""), h = new URLSearchParams(a);
          h.size && (c += (/\?/.test(c) ? "&" : "?") + h);
          let u = new URL(c, location.href);
          t2._htmx.preload = { prefetch: fetch(c, s2.request), action: u.origin === location.origin ? u.pathname + u.search : u.href, expiresAt: Date.now() + n };
          try {
            await t2._htmx.preload.prefetch;
          } catch (e2) {
            delete t2._htmx.preload;
          }
        };
        for (let e2 of i) t2.addEventListener(e2, s, { passive: true });
        t2._htmx.preloadListener = s, t2._htmx.preloadEvents = i;
      })(t);
    }, htmx_before_request: (e2, t) => {
      let { ctx: r } = t;
      if (e2._htmx?.preload && e2._htmx.preload.action === r.request.action && Date.now() < e2._htmx.preload.expiresAt) {
        let t2 = e2._htmx.preload.prefetch;
        r.fetch = () => t2, delete e2._htmx.preload;
      } else e2._htmx && delete e2._htmx.preload;
    }, htmx_before_cleanup: (e2) => {
      if (e2._htmx?.preloadListener) for (let t of e2._htmx.preloadEvents) e2.removeEventListener(t, e2._htmx.preloadListener);
    } });
  })(), (() => {
    if ("undefined" == typeof navigation) return;
    let e, t = 0, r = /* @__PURE__ */ new Set(), i = null;
    function n() {
      navigation.addEventListener("navigate", (e2) => {
        if (!e2.canIntercept) return;
        let n2, s2 = history.state;
        e2.intercept({ handler: () => new Promise((e3) => {
          n2 = e3;
        }), scroll: "manual", focusReset: "manual" }), e2.signal.addEventListener("abort", () => {
          t > 0 && (r.forEach((e3) => e3()), r.clear(), t = 0), i = null;
        }), i = () => {
          n2(), history.replaceState(s2, "");
        };
      }, { once: true }), navigation.navigate(location.href, { history: "replace" });
    }
    function s() {
      i && (i(), i = null);
    }
    htmx2.registerExtension("browser-indicator", { init: (t2) => {
      e = t2;
    }, htmx_before_history_update: () => {
      s();
    }, htmx_before_request: (i2, s2) => {
      (function(t2) {
        let r2 = e.attributeValue(t2, "hx-browser-indicator");
        return null != r2 && "false" !== r2 || !(!htmx2.config.boostBrowserIndicator || !t2._htmx?.boosted);
      })(i2) && (s2.ctx._browserIndicator = true, t++, 1 === t && n(), s2.ctx.request?.abort && r.add(s2.ctx.request.abort));
    }, htmx_finally_request: (e2, i2) => {
      i2.ctx._browserIndicator && (i2.ctx.request?.abort && r.delete(i2.ctx.request.abort), 0 !== t && (t--, 0 === t && s()));
    } });
  })(), (() => {
    let e;
    function t(t2, r, i) {
      return (async () => {
        let n = +r.headers.get("Content-Length") || null;
        e.triggerHtmxEvent(t2, "htmx:download:start", { total: n });
        let s = r.body.getReader(), o = [], a = 0;
        for (; ; ) {
          let { done: r2, value: i2 } = await s.read();
          if (r2) break;
          o.push(i2), a += i2.length, e.triggerHtmxEvent(t2, "htmx:download:progress", { loaded: a, total: n, percent: n ? Math.round(a / n * 100) : null });
        }
        let l = new Blob(o, { type: r.headers.get("Content-Type") || "application/octet-stream" }), c = (function(e2, t3) {
          let r2 = e2.get("Content-Disposition");
          if (r2) {
            let e3 = r2.match(/filename\*?=['"]?(?:UTF-8'')?([^'";]+)/i);
            if (e3) return decodeURIComponent(e3[1]);
          }
          return t3.split("/").pop().split("?")[0] || "download";
        })(r.headers, i), h = URL.createObjectURL(l);
        Object.assign(document.createElement("a"), { href: h, download: c }).click(), URL.revokeObjectURL(h), e.triggerHtmxEvent(t2, "htmx:download:complete", { filename: c, size: l.size });
      })();
    }
    htmx2.registerExtension("download", { init: (t2) => {
      e = t2;
    }, htmx_before_response: (e2, { ctx: r }) => {
      let i = r.response.headers.get("HX-Download");
      if (i) return void (async () => {
        t(r.sourceElement, await fetch(i), i);
      })();
      let n = r.response.headers.get("Content-Disposition");
      return "download" === r.swap || n?.includes("attachment") ? (clearTimeout(r.requestTimeout), r.extensionPromise = t(r.sourceElement, r.response.raw, r.request.action), false) : void 0;
    } });
  })(), (() => {
    let e;
    function t(e2) {
      if (e2.pendingDiv) {
        e2.pendingDiv.remove();
        for (let t2 of e2.pendingHidden) t2.style.display = "";
      }
    }
    htmx2.registerExtension("hx-pending", { init: (t2) => {
      e = t2;
    }, htmx_config_request: (e2, t2) => {
      let r = t2.ctx.request.body;
      r?.entries && (t2.ctx.pendingBody = r);
    }, htmx_before_request: (t2, r) => {
      !(function(t3) {
        if (t3.pending = e.attributeValue(t3.sourceElement, "hx-pending"), !t3.pending) return;
        let r2 = document.querySelector(t3.pending);
        if (!r2) return;
        let i = t3.target;
        if ("string" == typeof i && (i = document.querySelector(i)), !i) return;
        let n = document.createElement("div");
        n.style.cssText = "all: initial", n.classList.add("hx-pending");
        let s = r2 instanceof HTMLTemplateElement ? r2.content.childNodes : r2.childNodes;
        for (let e2 of s) n.appendChild(e2.cloneNode(true));
        if (t3.pendingBody) {
          let e2 = new Set(t3.pendingBody.keys());
          for (let r3 of e2) {
            let e3 = t3.pendingBody.getAll(r3).filter((e4) => "string" == typeof e4);
            if (!e3.length) continue;
            let i2 = 1 === e3.length ? e3[0] : JSON.stringify(e3);
            try {
              n.dataset[r3] = i2;
            } catch (e4) {
              try {
                n.setAttribute("data-" + r3, i2);
              } catch (e5) {
              }
            }
          }
        }
        let o = "before" === (a = t3.swap) ? "beforebegin" : "after" === a ? "afterend" : "prepend" === a ? "afterbegin" : "append" === a ? "beforeend" : a;
        var a;
        if (t3.pendingHidden = [], "innerHTML" === o) {
          for (let e2 of i.children) e2.style.display = "none", t3.pendingHidden.push(e2);
          i.appendChild(n);
        } else ["beforebegin", "afterbegin", "beforeend", "afterend"].includes(o) ? i.insertAdjacentElement(o, n) : (i.style.display = "none", t3.pendingHidden.push(i), i.after(n));
        t3.pendingDiv = n, htmx2.process(n);
      })(r.ctx);
    }, htmx_error: (e2, r) => {
      t(r.ctx);
    }, htmx_before_swap: (e2, r) => {
      t(r.ctx);
    } });
  })(), (() => {
    let e;
    htmx2.registerExtension("hx-targets", { init: (t) => {
      e = t;
    }, htmx_before_swap: (t, r) => {
      let { ctx: i, tasks: n } = r, s = e.attributeValue(i.sourceElement, "hx-targets");
      if (!s) return;
      let o = htmx2.findAll(i.sourceElement, s);
      if (!o.length) return void console.warn(`htmx: '${s}' on hx-targets did not match any elements`, { selector: s });
      let a = n.findIndex((e2) => "main" === e2.type);
      if (-1 === a) return;
      let l = n[a], c = Array.from(o).map((e2) => ({ ...l, fragment: l.fragment.cloneNode(true), target: e2 }));
      n.splice(a, 1, ...c);
    } });
  })(), (() => {
    let e, t = /* @__PURE__ */ new Set(), r = false, i = /* @__PURE__ */ Symbol(), n = null, s = null, o = null, a = 0, l = false;
    const c = { childList: true, subtree: true, attributes: true, characterData: true };
    let h = null;
    function u() {
      r || a > 0 || (r = true, queueMicrotask(() => {
        n?.disconnect();
        let e2 = performance.now();
        t.forEach((e3) => e3());
        let i2 = performance.now() - e2;
        !l && i2 > 16 && (console.warn(`htmx: hx-live expressions took ${i2.toFixed(1)}ms.`), l = true), 0 === t.size ? n && (clearTimeout(h), h = null, document.removeEventListener("input", o, true), o = null, document.removeEventListener("change", s, true), n.disconnect(), n = null, s = null, l = false) : n.observe(document.documentElement, c), r = false;
      }));
    }
    let d = new Set("disabled required readonly open inert multiple autofocus novalidate default reversed loop muted controls autoplay playsinline formnovalidate async defer ismap typemustmatch allowfullscreen itemscope nomodule alpha headingreset".split(" ")), f = new Set("checked value selected hidden".split(" ")), m = new Set("contenteditable draggable spellcheck writingsuggestions".split(" ")), p = new Set("tabindex colspan rowspan maxlength minlength size span start rows cols width height min max step low high optimum".split(" "));
    function g(e2, t2) {
      return e2 instanceof HTMLElement ? t2.toLowerCase() : t2;
    }
    function x(e2, t2) {
      if ((t2 = g(e2, t2)).startsWith("aria-")) return (function(e3, t3) {
        let r3 = e3?.getAttribute("aria-" + t3);
        return null == r3 ? void 0 : O.has(t3) ? r3 : N.has(t3) ? r3.trim() ? r3.trim().split(/\s+/) : [] : q(r3);
      })(e2, t2.slice(5));
      if (t2.startsWith("data-")) return (function(e3, t3) {
        let r3 = e3.getAttribute(t3);
        return null === r3 ? void 0 : q(r3);
      })(e2, t2);
      if ("value" === t2 && ("number" === e2.type || "range" === e2.type)) return "" === e2.value ? null : e2.valueAsNumber;
      if (f.has(t2)) return e2[t2];
      if (d.has(t2) || t2.startsWith("shadowroot") && "shadowrootmode" !== t2 && "shadowrootslotassignment" !== t2) return e2.hasAttribute(t2);
      let r2 = e2.getAttribute(t2);
      if (null != r2 && m.has(t2)) try {
        return JSON.parse(r2.toLowerCase());
      } catch {
      }
      return p.has(t2) && r2?.trim() && isFinite(r2) ? +r2 : r2;
    }
    function b(e2, t2, r2) {
      if (t2 = g(e2, t2), "function" == typeof r2 && (r2 = r2(x(e2, t2)), "function" == typeof r2?.then)) throw new TypeError("hx-live: assignment returned a promise");
      t2.startsWith("aria-") ? (function(e3, t3, r3) {
        let i2 = "aria-" + t3;
        null == r3 ? e3.removeAttribute(i2) : e3.setAttribute(i2, N.has(t3) && Array.isArray(r3) ? r3.join(" ") : String(r3));
      })(e2, t2.slice(5), r2) : t2.startsWith("data-") ? (function(e3, t3, r3) {
        void 0 === r3 ? e3.removeAttribute(t3) : e3.setAttribute(t3, "object" == typeof r3 || q(r3) !== r3 ? JSON.stringify(r3) : r3);
      })(e2, t2, r2) : f.has(t2) ? (function(e3, t3, r3) {
        if ("checked" === t3 || "selected" === t3) {
          let i2 = !!r3;
          e3[t3] = i2, e3.toggleAttribute(t3, i2);
        } else false === r3 || null == r3 ? (e3[t3] = "boolean" != typeof e3[t3] && "", e3.removeAttribute(t3)) : true === r3 ? (e3[t3] = true, e3.setAttribute(t3, "")) : (e3[t3] = r3, e3.setAttribute(t3, String(r3)));
      })(e2, t2, r2) : d.has(t2) || t2.startsWith("shadowroot") && "shadowrootmode" !== t2 && "shadowrootslotassignment" !== t2 ? e2.toggleAttribute(t2, !!r2) : null == r2 ? e2.removeAttribute(t2) : e2.setAttribute(t2, String(r2));
    }
    function y(e2, t2) {
      return t2 ? new Set(e2.map(t2)) : e2;
    }
    function v(e2, t2) {
      let r2 = e2.prefix;
      return r2 + ("data-" === r2 ? C(t2) : r2 ? t2.toLowerCase() : t2);
    }
    function w(e2, t2, r2) {
      if (!e2.cascades) return e2.prefix ? t2.hasAttribute(r2) && t2 : t2;
      for (; t2 && !t2.hasAttribute(r2); ) t2 = t2.parentElement;
      return t2;
    }
    function S(e2, t2, r2, i2) {
      if ("string" != typeof t2) return false;
      let n2 = v(e2, t2);
      return y(e2.elts, e2.cascades && ((t3) => w(e2, t3, n2) || !i2 && t3)).forEach((e3) => e3 && b(e3, n2, r2)), true;
    }
    let E = { get: (e2, t2) => {
      if (!e2.prefix && "class" === t2) return e2.scope.class;
      if ("string" != typeof t2) return;
      let r2 = v(e2, t2), i2 = e2.elts[0] && w(e2, e2.elts[0], r2);
      return i2 ? x(i2, r2) : void 0;
    }, set: (e2, t2, r2) => S(e2, t2, r2), deleteProperty: (e2, t2) => S(e2, t2, void 0, true), has: (e2, t2) => "data-" === e2.prefix && "string" == typeof t2 && !!e2.elts[0] && !!w(e2, e2.elts[0], v(e2, t2)), ownKeys: (e2) => {
      if ("data-" !== e2.prefix) return [];
      let t2 = [], r2 = /* @__PURE__ */ new Set();
      for (let i2 = e2.elts[0]; i2; i2 = e2.cascades ? i2.parentElement : null) for (let e3 of Object.keys(i2.dataset)) "htmxPowered" === e3 || r2.has(e3) || (r2.add(e3), t2.push(e3));
      return t2;
    }, getOwnPropertyDescriptor: (e2, t2) => {
      if ("data-" === e2.prefix && "string" == typeof t2 && "htmxPowered" !== t2) return e2.elts[0] && w(e2, e2.elts[0], v(e2, t2)) ? { enumerable: true, configurable: true } : void 0;
    } };
    function A(e2, t2, r2, i2 = "") {
      return new Proxy({ elts: e2, cascades: t2, scope: r2, prefix: i2 }, E);
    }
    function C(e2) {
      return e2.replace(/[A-Z]/g, (e3) => "-" + e3.toLowerCase());
    }
    function q(e2) {
      try {
        return JSON.parse(e2);
      } catch {
        return e2;
      }
    }
    let T = /(['"`\/])(?:\\.|(?!\1).)*\1|(?<![.\w$#])class(?=\s*[.?[])/gs;
    function _(e2) {
      return e2.replace(T, (e3, t2) => t2 ? "`" === t2 ? e3.replace(/\$\{((?:[^{}]|\{[^{}]*\})*)\}/g, (e4, t3) => "${" + _(t3) + "}") : e3 : "attr.class");
    }
    let O = new Set("activedescendant details errormessage keyshortcuts label placeholder roledescription valuetext".split(" ")), N = new Set("controls describedby dropeffect flowto labelledby owns relevant".split(" "));
    function H(e2, t2) {
      return !!e2?.classList.contains(t2);
    }
    function k(e2, t2, r2) {
      if ("function" == typeof r2 && (r2 = r2(H(e2, t2)), "function" == typeof r2?.then)) throw new TypeError("hx-live: assignment returned a promise");
      e2.classList.toggle(t2, !!r2), e2.classList.length || e2.removeAttribute("class");
    }
    function M(e2, t2) {
      let r2, i2, n2, s2, o2 = { get data() {
        return r2 ||= A(e2, t2, null, "data-");
      }, get aria() {
        return i2 ||= A(e2, t2, null, "aria-");
      }, get class() {
        return n2 ||= (function(e3, t3 = false) {
          let r3 = e3[0], i3 = (e4, r4) => t3 ? e4.closest("." + CSS.escape(r4)) : e4, n3 = (e4) => H(r3 && i3(r3, e4), e4), s3 = (t4, r4) => y(e3, (e4) => i3(e4, t4) || e4).forEach((e4) => k(e4, t4, r4)), o3 = { assign(t4) {
            t4 && "object" == typeof t4 && !Array.isArray(t4) ? P(s3, t4) : console.warn("hx-live: class.assign expects an object.", { elts: e3 });
          }, add: (...e4) => e4.forEach((e5) => s3(e5, true)), remove: (...e4) => e4.forEach((e5) => s3(e5, false)), contains: n3, toggle(e4, t4) {
            let r4 = t4 ?? !n3(e4);
            return s3(e4, (e5) => t4 ?? !e5), r4;
          }, replace(r4, i4) {
            if (t3) return !!n3(r4) && (s3(r4, false), s3(i4, true), true);
            let o4;
            for (let t4 = 0; t4 < e3.length; t4++) {
              let n4 = e3[t4].classList.replace(r4, i4);
              0 === t4 && (o4 = n4);
            }
            return o4;
          } };
          return new Proxy({}, { get: (e4, i4) => {
            if ("string" == typeof i4 && o3[i4]) return o3[i4];
            let s4 = !t3 && r3?.classList;
            if (s4 && i4 in s4) {
              let e5 = s4[i4];
              return "function" == typeof e5 ? e5.bind(s4) : e5;
            }
            return r3 ? "string" == typeof i4 ? n3(i4) : void 0 : i4 === Symbol.iterator ? () => [][Symbol.iterator]() : void 0;
          }, set: (r4, i4, n4) => {
            if ("string" != typeof i4) return false;
            if (t3 || "value" !== i4) s3(i4, n4);
            else for (let t4 of e3) t4.classList.value = n4;
            return true;
          }, deleteProperty: (e4, t4) => "string" == typeof t4 && (s3(t4, false), true), has: (e4, t4) => "string" == typeof t4 && n3(t4), ownKeys: () => !t3 && r3 ? [...r3.classList] : [], getOwnPropertyDescriptor: (e4, r4) => !t3 && n3(r4) ? { enumerable: true, configurable: true } : void 0 });
        })(e2, t2);
      }, get attr() {
        return s2 ||= A(e2, t2, o2);
      } };
      return o2;
    }
    function I(e2) {
      let t2;
      return new Proxy(function() {
      }, { apply: (t3, r2, [i2]) => {
        let n2 = /* @__PURE__ */ new Set();
        for (let t4 of e2) {
          let e3 = t4.closest?.(i2);
          e3 && n2.add(e3);
        }
        return X([...n2]);
      }, get: (r2, i2) => (t2 ||= M(e2, true))[i2] });
    }
    function L(t2, r2, i2) {
      "class" === r2 ? (function(t3, r3) {
        let i3 = e.htmxProp(t3), n2 = i3.liveClasses || /* @__PURE__ */ new Set(), s2 = new Set(P((e2, r4) => k(t3, e2, r4), r3));
        for (let e2 of n2) s2.has(e2) || k(t3, e2, false);
        i3.liveClasses = s2;
      })(t2, i2) : k(t2, r2.slice(1), i2);
    }
    function P(e2, t2) {
      let r2 = [];
      if ("string" == typeof t2 && (t2 = { [t2]: true }), t2 && "object" == typeof t2) for (let [i2, n2] of Object.entries(t2)) for (let t3 of i2.trim().split(/\s+/).filter(Boolean)) r2.push(t3), e2(t3, !!n2);
      return r2;
    }
    function D(e2, t2, r2) {
      let i2 = t2.startsWith("."), n2 = i2 ? t2.slice(1) : t2, s2 = t2.startsWith("aria-"), o2 = i2 ? "." + n2 : "[" + t2 + "]", a2 = null == r2 ? e2[0]?.parentElement : r2.nodeType ? r2 : null, l2 = a2 ? [a2, ...a2.querySelectorAll(o2)] : e2.length ? document.querySelectorAll("string" == typeof r2 ? r2 : r2?.from || o2) : [], c2 = new Set(e2);
      for (let e3 of l2) c2.has(e3) || (i2 ? (e3.classList?.remove(n2), 0 === e3.classList?.length && e3.removeAttribute("class")) : s2 ? e3.setAttribute(t2, "false") : e3.removeAttribute(t2));
      for (let r3 of e2) i2 ? r3.classList?.add(n2) : s2 ? r3.setAttribute(t2, "true") : r3.setAttribute(t2, "");
    }
    function j(e2, ...t2) {
      let r2 = e2 || document;
      for (let e3 of t2) e3?.addEventListener && (r2 = e3);
      return new Promise((e3) => {
        let i2 = [], n2 = false, s2 = (t3) => {
          if (!n2) {
            n2 = true;
            for (let e4 of i2) e4();
            e3(t3);
          }
        };
        for (let e4 of t2) {
          if (null == e4 || e4?.addEventListener) continue;
          let t3 = "number" == typeof e4 ? e4 : "string" == typeof e4 ? htmx2.parseInterval(e4) : void 0;
          if (t3 > 0) {
            let r3 = setTimeout(() => s2(e4), t3);
            i2.push(() => clearTimeout(r3));
          } else "string" == typeof e4 && (r2.addEventListener(e4, s2, { once: true }), i2.push(() => r2.removeEventListener(e4, s2)));
        }
      });
    }
    function V(e2, t2, ...r2) {
      let i2 = t2.startsWith("."), n2 = i2 ? t2.slice(1) : t2, s2 = t2.startsWith("aria-"), o2 = r2.length > 1 ? r2 : r2[0];
      if ("string" == typeof o2 && (o2 = o2.split("|").map((e3) => e3.trim())), o2) if (i2) {
        let t3 = o2.findIndex((t4) => t4 && e2.classList.contains(t4));
        t3 >= 0 && e2.classList.remove(o2[t3]);
        let r3 = o2[(t3 + 1) % o2.length];
        r3 && e2.classList.add(r3);
      } else {
        let r3 = o2.indexOf(x(e2, t2) ?? ""), i3 = o2[(r3 + 1) % o2.length];
        b(e2, t2, "" === i3 ? void 0 : i3);
      }
      else if (i2) e2.classList.toggle(n2);
      else if (s2) {
        let r3 = e2.getAttribute(t2);
        e2.setAttribute(t2, "true" === r3 ? "false" : "true");
      } else e2.toggleAttribute(t2);
    }
    function R() {
      let e2 = /* @__PURE__ */ new Map();
      return (t2, r2) => {
        let n2 = (s2 = r2 ? r2.toString() : null, e2.get(s2) || (e2.set(s2, { last: 0, reject: null }), e2.get(s2)));
        var s2;
        n2.reject?.(i), n2.reject = null;
        let o2 = ++n2.last;
        if (!r2) return new Promise((e3, r3) => {
          n2.reject = r3, setTimeout(() => {
            o2 === n2.last && (n2.reject = null, e3());
          }, t2);
        });
        setTimeout(() => o2 === n2.last && r2(), t2);
      };
    }
    function W(t2) {
      let r2 = e.htmxProp(t2);
      return r2.debounce || (r2.debounce = R());
    }
    function $(e2, t2 = document) {
      return (r2) => {
        if ("string" != typeof r2) return X(r2?.nodeType ? [r2] : [...r2 || []]);
        let i2 = r2, n2 = i2.match(/^(.+)\s+in\s+(.+)$/), s2 = [t2];
        if (n2 && (i2 = n2[1], s2 = "this" === n2[2] || "me" === n2[2] ? [e2] : [...document.querySelectorAll(n2[2])]), !s2.length) return X([]);
        let o2, a2 = (e3) => {
          if (1 === s2.length) return [...s2[0].querySelectorAll(e3)];
          let t3 = [], r3 = /* @__PURE__ */ new Set();
          for (let i3 of s2) for (let n3 of i3.querySelectorAll(e3)) r3.has(n3) || (r3.add(n3), t3.push(n3));
          return t3.sort((e4, t4) => 4 & e4.compareDocumentPosition(t4) ? -1 : 1);
        }, l2 = i2.match(/^(next|previous|closest|first|last)\s+(.+)$/);
        if (l2) {
          let [, t3, r3] = l2, i3 = (t4) => e2.compareDocumentPosition(t4);
          if ("closest" === t3) {
            let t4 = e2.closest?.(r3);
            o2 = t4 ? [t4] : [];
          } else {
            let e3 = a2(r3);
            if ("first" === t3) o2 = e3.slice(0, 1);
            else if ("last" === t3) o2 = e3.slice(-1);
            else if ("next" === t3) {
              let t4 = e3.find((e4) => 4 & i3(e4));
              o2 = t4 ? [t4] : [];
            } else {
              let t4 = e3.reverse().find((e4) => 2 & i3(e4));
              o2 = t4 ? [t4] : [];
            }
          }
        } else o2 = a2(i2);
        return X(o2);
      };
    }
    let B = "map filter reduce reduceRight forEach some every find findIndex findLast findLastIndex flatMap flat slice indexOf lastIndexOf includes join at".split(" "), F = { before: "beforebegin", after: "afterend", start: "afterbegin", end: "beforeend" };
    function z(e2, t2, r2) {
      let i2 = e2.parentElement;
      "into" === t2 ? e2.innerHTML = r2 : "replace" === t2 ? e2.outerHTML = r2 : e2.insertAdjacentHTML(F[t2], r2), htmx2.process(i2);
    }
    let U, J, Q, G = () => {
    };
    function X(e2) {
      let t2, r2, i2 = new Proxy({}, { get: (n2, s2) => {
        if ("count" === s2) return e2.length;
        if ("arr" === s2) return () => e2.slice();
        if (s2 === Symbol.iterator) return () => e2.values();
        if ("q" === s2) return (t3) => {
          let r3 = /* @__PURE__ */ new Set();
          for (let i3 of e2) for (let e3 of $(i3, i3)(t3).arr()) r3.add(e3);
          return X([...r3]);
        };
        if ("trigger" === s2) return (t3, r3, n3) => (e2.forEach((e3) => htmx2.trigger(e3, t3, r3, n3)), i2);
        if ("insert" === s2) return (t3, r3) => (e2.forEach((e3) => z(e3, t3, r3)), i2);
        if ("take" === s2) return (t3, r3) => (D(e2, t3, r3), i2);
        if ("toggle" === s2) return (t3, ...r3) => (e2.forEach((e3) => V(e3, t3, ...r3)), i2);
        if ("attr" === s2 || "class" === s2 || "aria" === s2) return (t2 ||= M(e2, false))[s2];
        if ("data" === s2) return (r2 ||= I(e2)).data;
        if ("local" === s2) return t2 ||= M(e2, false);
        if ("closest" === s2) return r2 ||= I(e2);
        if (B.includes(s2)) return e2[s2].bind(e2);
        if (!e2.length) return "function" == typeof (function(e3) {
          for (let t3 = HTMLElement.prototype; t3; t3 = Object.getPrototypeOf(t3)) {
            let r3 = Object.getOwnPropertyDescriptor(t3, e3);
            if (r3) return r3;
          }
        })(s2)?.value ? G : void 0;
        let o2 = e2[0]?.[s2];
        return "function" == typeof o2 ? (...t3) => e2.map((e3) => e3[s2](...t3))[0] : o2 && "object" == typeof o2 ? X(e2.map((e3) => e3[s2])) : o2;
      }, set: (t3, r3, i3) => (e2.forEach((e3) => {
        let t4 = e3[r3];
        if (null == t4 || "function" == typeof t4) e3[r3] = i3;
        else if ("function" == typeof i3) {
          let n2 = i3(t4);
          if ("function" == typeof n2?.then) throw new TypeError("hx-live: assignment returned a promise");
          e3[r3] = n2;
        } else e3[r3] = i3;
      }), u(), true) });
      return i2;
    }
    function K() {
      let e2 = htmx2.config.metaCharacter || ":", t2 = htmx2.config.prefix;
      J = ["hx-live" + e2], t2 && J.push(t2 + "live" + e2);
      let r2 = htmx2.config.live?.bindPrefix;
      void 0 === r2 && (window.Alpine ? (r2 = "", console.warn("hx-live: Alpine detected; set config.live.bindPrefix.")) : r2 = ":"), r2 && J.push(r2), Q = ["hx-live"], t2 && Q.push(t2 + "live");
      let i2 = J.map((e3) => `starts-with(name(), "${e3}")`).join(" or "), n2 = Q.map((e3) => `@${e3}`).join(" or ");
      U = new XPathEvaluator().createExpression(`.//*[@*[${i2}] or ${n2}]`);
    }
    function Y(e2) {
      for (let t2 of J) if (e2.startsWith(t2) && e2.length > t2.length) return e2.slice(t2.length);
    }
    function Z(e2) {
      let r2 = e2._htmx;
      if (r2?.liveRuns) {
        for (let e3 of r2.liveRuns) t.delete(e3);
        delete r2.liveRuns, delete r2.effectRegistered, delete r2.bindings;
      }
    }
    function ee(t2) {
      if (t2.closest("[hx-ignore]")) return;
      let r2 = e.htmxProp(t2);
      if (!r2.effectRegistered) {
        let e2 = Q.find((e3) => t2.hasAttribute(e3));
        e2 && (r2.effectRegistered = true, te(t2, t2.getAttribute(e2)));
      }
      r2.bindings ||= /* @__PURE__ */ new Set();
      for (let e2 of t2.attributes) {
        let i2 = Y(e2.name);
        i2 && !r2.bindings.has(i2) && (r2.bindings.add(i2), te(t2, e2.value, i2));
      }
    }
    function te(r2, a2, l2) {
      !(function() {
        if (n) return;
        s = () => u();
        let e2 = htmx2.parseInterval(htmx2.config.live?.inputDebounce ?? 100) ?? 100;
        o = () => {
          clearTimeout(h), h = setTimeout(u, e2);
        }, document.addEventListener("input", o, true), document.addEventListener("change", s, true), n = new MutationObserver(s), n.observe(document.documentElement, c);
      })();
      let d2, f2 = void 0 !== l2, m2 = W(r2), p2 = /\bawait\b/.test(a2), g2 = !f2 && p2, y2 = !f2 || p2, v2 = false, w2 = async () => {
        if (r2.isConnected) {
          if (!g2 || !v2) {
            v2 = g2;
            try {
              d2 ||= e.executeJavaScript(r2, { debounce: m2 }, a2, f2, y2, true);
              let t2 = y2 ? await d2() : d2();
              f2 && (!(function(t3, r3, i2) {
                if ("function" == typeof i2) throw new TypeError("hx-live: binding returned a function");
                if ("text" === r3) {
                  let e2 = null == i2 ? "" : String(i2);
                  return void (t3.textContent !== e2 && (t3.textContent = e2));
                }
                if ("html" === r3) {
                  let e2 = null == i2 ? "" : String(i2);
                  return void (t3.innerHTML !== e2 && (t3.innerHTML = e2));
                }
                if ("style" === r3) return void (function(t4, r4) {
                  let i3 = e.htmxProp(t4), n2 = i3.liveStyles || /* @__PURE__ */ new Set(), s2 = [];
                  if ("string" == typeof r4) for (let e2 of r4.split(";")) {
                    let t5 = e2.indexOf(":");
                    if (t5 < 0) continue;
                    let r5 = e2.slice(0, t5).trim(), i4 = e2.slice(t5 + 1).trim();
                    r5 && s2.push([r5, i4]);
                  }
                  else if (r4 && "object" == typeof r4) for (let [e2, t5] of Object.entries(r4)) s2.push([C(e2), null == t5 || "" === t5 ? null : String(t5)]);
                  let o2 = new Set(s2.map(([e2]) => e2));
                  for (let e2 of n2) o2.has(e2) || t4.style.removeProperty(e2);
                  for (let [e2, r5] of s2) null == r5 ? t4.style.removeProperty(e2) : t4.style.setProperty(e2, r5);
                  0 === t4.style.length && t4.removeAttribute("style"), i3.liveStyles = o2;
                })(t3, i2);
                if ("class" === r3 || r3.startsWith(".")) return void L(t3, r3, i2);
                if (x(t3, r3) === i2) return;
                b(t3, r3, i2);
              })(r2, l2, t2), y2 && n?.takeRecords());
            } catch (e2) {
              e2 !== i && console.error("hx-live expression failed", e2, f2 ? { elt: r2, attr: l2 } : { elt: r2 });
            } finally {
              g2 && queueMicrotask(() => v2 = false);
            }
          }
        } else t.delete(w2);
      };
      t.add(w2);
      let S2 = e.htmxProp(r2);
      S2.liveRuns = S2.liveRuns || /* @__PURE__ */ new Set(), S2.liveRuns.add(w2), w2();
    }
    htmx2.live = { q: (e2) => $(document.documentElement)(e2), debounce: R(), refresh: () => u(), take: (e2, t2, r2) => D(htmx2.live.q(e2).arr(), t2, r2), toggle: (e2, t2, ...r2) => htmx2.live.q(e2).forEach((e3) => V(e3, t2, ...r2)), forEvent: (...e2) => j(null, ...e2), nextFrame: () => new Promise((e2) => requestAnimationFrame(e2)) }, htmx2.live.$ = htmx2.live.q, htmx2.registerExtension("hx-live", { init: (t2) => {
      e = t2;
    }, htmx_before_cleanup: (e2) => {
      Z(e2);
    }, htmx_before_morph_attr: (e2, t2) => {
      U || K(), J.some((e3) => t2.attrName.startsWith(e3)) && Z(e2);
    }, htmx_after_process: (e2) => {
      !(function(e3) {
        U || K(), 1 === e3.nodeType && ee(e3);
        let t2, r2 = U.evaluate(e3), i2 = [];
        for (; t2 = r2.iterateNext(); ) i2.push(t2);
        for (t2 of i2) ee(t2);
      })(e2);
    }, htmx_before_swap: () => {
      a++;
    }, htmx_finally_swap: () => {
      0 === --a && t.size > 0 && u();
    }, htmx_scope: (t2, r2) => {
      let i2 = e.htmxProp(t2);
      Object.assign(r2.scope, i2.liveScope ||= (function(e2) {
        let t3 = M([e2], false), r3 = I([e2]);
        return { q: $(e2), forEvent: (...t4) => j(e2, ...t4), nextFrame: () => new Promise((e3) => requestAnimationFrame(e3)), trigger: (t4, r4, i3) => htmx2.trigger(e2, t4, r4, i3), debounce: W(e2), take: (t4, r4) => D([e2], t4, r4), toggle: (t4, ...r4) => V(e2, t4, ...r4), attr: t3.attr, insert: (t4, r4) => z(e2, t4, r4), matches: (t4) => e2.matches(t4), style: e2.style, data: r3.data, aria: t3.aria, local: t3, closest: r3 };
      })(t2)), r2.code = _(r2.code), htmx2.config.live?.useDollar && (r2.scope.$ = r2.scope.q);
    } });
  })(), (() => {
    let e, t = null;
    const r = "htmx-history-index";
    function i() {
      return htmx2.config.historyCache;
    }
    function n(e2) {
      let t2 = [e2];
      return htmx2.config.prefix && t2.push(e2.replaceAll("hx-", htmx2.config.prefix)), t2.join(",");
    }
    function s() {
      try {
        return sessionStorage.setItem("__htmx_test__", "1"), sessionStorage.removeItem("__htmx_test__"), true;
      } catch (e2) {
        return false;
      }
    }
    function o() {
      return htmx2.find(n("[hx-history-elt]")) || document.body;
    }
    const a = "data-htmx-history-value", l = "data-htmx-history-checked", c = "data-htmx-history-scroll";
    function h() {
      let e2 = history.state || { htmx: true }, r2 = e2.htmxId;
      r2 || (r2 = crypto.randomUUID?.() ?? Math.random().toString(36).slice(2), history.replaceState({ ...e2, htmxId: r2 }, "", location.href)), t = r2;
    }
    function u(e2) {
      sessionStorage.setItem(r, JSON.stringify(e2));
    }
    function d(e2, t2) {
      if (!s() || i().size <= 0) return false;
      let n2 = (function() {
        try {
          return JSON.parse(sessionStorage.getItem(r)) || [];
        } catch {
          return [];
        }
      })().filter((t3) => t3 !== e2);
      for (; n2.length >= i().size; ) sessionStorage.removeItem("htmx-history-" + n2.shift());
      let o2 = JSON.stringify(t2);
      for (; ; ) try {
        return sessionStorage.setItem("htmx-history-" + e2, o2), n2.push(e2), u(n2), true;
      } catch (e3) {
        if (0 === n2.length) return false;
        sessionStorage.removeItem("htmx-history-" + n2.shift());
      }
    }
    function f(r2) {
      if (!(r2 = r2 || t)) return;
      if (htmx2.find(n('[hx-history="false"]'))) return;
      let i2 = o();
      var s2;
      (s2 = i2).querySelectorAll("input, textarea, select").forEach((e2) => {
        let t2 = e2.type?.toLowerCase();
        if ("file" !== t2 && "password" !== t2) if ("checkbox" === t2 || "radio" === t2) e2.checked && e2.setAttribute(l, "1");
        else if ("SELECT" === e2.tagName && e2.multiple) {
          let t3 = Array.from(e2.options).filter((e3) => e3.selected).map((e3) => e3.value);
          t3.length && e2.setAttribute(a, JSON.stringify(t3));
        } else e2.value && e2.setAttribute(a, e2.value);
      }), s2.querySelectorAll("*").forEach((e2) => {
        (e2.scrollTop > 0 || e2.scrollLeft > 0) && e2.setAttribute(c, `${e2.scrollTop},${e2.scrollLeft}`);
      });
      let h2 = document.head.outerHTML, u2 = window.scrollY, f2 = document.title, m2 = { target: i2, head: h2 };
      if (false === e.triggerHtmxEvent(document, "htmx:history:cache:before:save", m2)) return;
      let p2 = (function(e2) {
        let t2 = e2.cloneNode(true);
        return t2.querySelectorAll(".htmx-request").forEach((e3) => e3.classList.remove("htmx-request")), t2.querySelectorAll("[disabled][data-disabled-by-htmx]").forEach((e3) => {
          e3.removeAttribute("disabled"), e3.removeAttribute("data-disabled-by-htmx");
        }), t2.outerHTML;
      })(i2);
      h2 = m2.head, d(r2, { content: p2, head: h2, scroll: u2, title: f2 }), e.triggerHtmxEvent(document, "htmx:history:cache:after:save", { content: p2, head: h2, scroll: u2, title: f2 });
    }
    function m() {
      let e2 = history.state?.htmxId;
      return e2 && s() ? (function(e3) {
        try {
          return JSON.parse(sessionStorage.getItem("htmx-history-" + e3));
        } catch {
          return null;
        }
      })(e2) : null;
    }
    async function p(t2) {
      let r2 = { head: t2.head, ready: null };
      e.triggerHtmxEvent(document, "htmx:history:cache:before:restore", r2), r2.ready && await r2.ready;
      let n2 = t2.content, s2 = o(), h2 = i().swapStyle, u2 = { sourceElement: document.body, target: s2, swap: h2, text: n2, transition: false };
      await htmx2.swap(u2), document.title = t2.title || document.title, requestAnimationFrame(() => {
        var i2;
        window.scrollTo(0, t2.scroll || 0), (i2 = o()).querySelectorAll(`[${l}]`).forEach((e2) => {
          e2.checked = true, e2.removeAttribute(l);
        }), i2.querySelectorAll(`[${a}]`).forEach((e2) => {
          let t3 = e2.getAttribute(a);
          if ("SELECT" === e2.tagName && e2.multiple) {
            let r3 = JSON.parse(t3);
            Array.from(e2.options).forEach((e3) => {
              e3.selected = r3.includes(e3.value);
            });
          } else e2.value = t3;
          e2.removeAttribute(a);
        }), i2.querySelectorAll(`[${c}]`).forEach((e2) => {
          let [t3, r3] = e2.getAttribute(c).split(",").map(Number);
          e2.scrollTop = t3, e2.scrollLeft = r3, e2.removeAttribute(c);
        }), r2.item = t2, e.triggerHtmxEvent(document, "htmx:history:cache:after:restore", r2);
      });
    }
    htmx2.registerExtension("history-cache", { init: (t2) => {
      e = t2, htmx2.config.historyCache ??= {}, htmx2.config.historyCache.size ??= 10, htmx2.config.historyCache.refreshOnMiss ??= false, htmx2.config.historyCache.disable ??= false, htmx2.config.historyCache.swapStyle ??= "outerSync", h();
    }, htmx_before_history_update: (e2, t2) => {
      i().disable || (h(), f());
    }, htmx_after_history_update: () => {
      i().disable || h();
    }, htmx_before_history_restore: (r2, n2) => {
      if (i().disable) return;
      let s2 = t, o2 = history.state?.htmxId;
      s2 && s2 !== o2 && f(s2), o2 ? t = o2 : h();
      let a2 = m();
      if (!a2) {
        let t2 = { path: n2.path, refreshOnMiss: i().refreshOnMiss };
        return e.triggerHtmxEvent(document, "htmx:history:cache:miss", t2), t2.refreshOnMiss ? (location.reload(), false) : void 0;
      }
      let l2 = { path: n2.path, item: a2 };
      return false !== e.triggerHtmxEvent(document, "htmx:history:cache:hit", l2) ? (n2.cancelled = true, p(l2.item), false) : void 0;
    } });
  })(), (() => {
    let e;
    htmx2.registerExtension("upsert", { init: (t) => {
      e = t;
    }, htmx_process_upsert: (t, r) => {
      let { ctx: i, tasks: n } = r, s = { style: "upsert" }, o = t.getAttribute("key"), a = t.getAttribute("sort"), l = t.hasAttribute("prepend");
      o && (s.key = o), null !== a && (s.sort = a || true), l && (s.prepend = true), n.push({ type: "partial", fragment: t.content.cloneNode(true), target: e.attributeValue(t, "hx-target"), swapSpec: s, sourceElement: i.sourceElement });
    }, handle_swap: (e2, t, r, i) => {
      if ("upsert" === e2) {
        let e3 = i.key || "id", n = "desc" === i.sort, s = t.firstChild, o = (t2) => t2.getAttribute(e3) || t2.id, a = (e4, t2) => {
          let r2 = e4.localeCompare(t2, void 0, { numeric: true });
          return n ? -r2 : r2;
        };
        for (let e4 of Array.from(r.children)) {
          let r2 = e4.id;
          if (r2) {
            let t2 = document.getElementById(r2);
            if (t2) {
              t2.replaceWith(e4);
              continue;
            }
          }
          let n2 = o(e4);
          if (!n2) {
            i.prepend ? t.insertBefore(e4, s) : t.appendChild(e4);
            continue;
          }
          let l = false;
          for (let r3 of t.children) {
            let i2 = o(r3);
            if (i2 && a(n2, i2) < 0) {
              t.insertBefore(e4, r3), l = true;
              break;
            }
          }
          l || t.appendChild(e4);
        }
        return true;
      }
      return false;
    } });
  })(), (() => {
    let e, t = 0;
    function r() {
      t > 0 && t--, 0 === t && window.Alpine?.flushAndStopDeferringMutations && window.Alpine.flushAndStopDeferringMutations();
    }
    htmx2.registerExtension("alpine-compat", { init: (t2) => {
      e = t2;
      let r2 = e.isSoftMatch;
      e.isSoftMatch = function(e2, t3) {
        return e2._x_bindings?.id && t3.matches?.("[\\:id], [x-bind\\:id]") ? e2 instanceof Element && e2.tagName === t3.tagName : r2(e2, t3);
      };
    }, htmx_before_swap: (e2, r2) => {
      if (!window.Alpine?.closestDataStack || !window.Alpine?.cloneNode || !window.Alpine?.deferMutations) return;
      0 === t && window.Alpine.deferMutations(), t++;
      let { tasks: i } = r2;
      for (let e3 of i) if ("innerMorph" === e3.swapSpec.style || "outerMorph" === e3.swapSpec.style) {
        if (!e3.fragment || !e3.target) continue;
        if (!("string" == typeof e3.target ? document.querySelector(e3.target) : e3.target)) continue;
      }
    }, htmx_before_morph_node: (t2, r2) => {
      if (!window.Alpine?.closestDataStack || !window.Alpine?.cloneNode) return;
      let { oldNode: i, newNode: n } = r2, s = window.Alpine.closestDataStack(i);
      if (n._x_dataStack = s, i.isConnected && (window.Alpine.cloneNode(i, n), i._x_teleport && n._x_teleport)) {
        let t3 = document.createDocumentFragment();
        t3.append(n._x_teleport), e.morph(i._x_teleport, t3, false);
      }
    }, htmx_history_cache_before_save: (e2, t2) => {
      window.Alpine?.destroyTree && (t2.target.querySelectorAll("[x-data]").forEach((e3) => {
        e3._x_dataStack && e3.setAttribute("data-alpine-state", JSON.stringify(e3._x_dataStack[0]));
      }), window.Alpine.destroyTree(t2.target));
    }, htmx_history_cache_after_restore: (e2, t2) => {
      window.Alpine && document.querySelectorAll("[data-alpine-state]").forEach((e3) => {
        let t3 = JSON.parse(e3.getAttribute("data-alpine-state"));
        if (e3.removeAttribute("data-alpine-state"), e3._x_dataStack) for (let r2 in t3) try {
          e3._x_dataStack[0][r2] = t3[r2];
        } catch {
        }
      });
    }, htmx_after_swap: (e2, t2) => {
      t2.ctx._alpineFlushed = true, r();
    }, htmx_finally_request: (e2, t2) => {
      t2.ctx._alpineFlushed || r();
    } });
  })();

  // node_modules/alpinejs/dist/module.esm.js
  var flushPending = false;
  var flushing = false;
  var queue = [];
  var lastFlushedIndex = -1;
  var queueNeedsSort = false;
  var transactionActive = false;
  function scheduler(callback) {
    queueJob(callback);
  }
  function startTransaction() {
    transactionActive = true;
  }
  function commitTransaction() {
    transactionActive = false;
    queueFlush();
  }
  function queueJob(job) {
    if (!queue.includes(job)) {
      queue.push(job);
      if (job._x_schedulerPriority !== void 0)
        queueNeedsSort = true;
    }
    queueFlush();
  }
  function dequeueJob(job) {
    let index = queue.indexOf(job);
    if (index !== -1 && index > lastFlushedIndex)
      queue.splice(index, 1);
  }
  function queueFlush() {
    if (!flushing && !flushPending) {
      if (transactionActive)
        return;
      flushPending = true;
      queueMicrotask(flushJobs);
    }
  }
  function flushJobs() {
    flushPending = false;
    flushing = true;
    for (let i = 0; i < queue.length; i++) {
      if (queueNeedsSort)
        sortPendingJobs(i);
      queue[i]();
      lastFlushedIndex = i;
    }
    queue.length = 0;
    lastFlushedIndex = -1;
    queueNeedsSort = false;
    flushing = false;
  }
  function sortPendingJobs(start2) {
    let depths = /* @__PURE__ */ new Map();
    let sorted = queue.slice(start2).sort((a, b) => compareJobs(a, b, depths));
    for (let i = 0; i < sorted.length; i++) {
      queue[start2 + i] = sorted[i];
    }
    queueNeedsSort = false;
  }
  function compareJobs(a, b, depths) {
    if (!isStructural(a))
      return isStructural(b) ? 1 : 0;
    if (!isStructural(b))
      return -1;
    let depthDifference = getElementDepth(a._x_schedulerPriority.el, depths) - getElementDepth(b._x_schedulerPriority.el, depths);
    return depthDifference || a._x_schedulerPriority.order - b._x_schedulerPriority.order;
  }
  function isStructural(job) {
    return job._x_schedulerPriority !== void 0;
  }
  function getElementDepth(el, depths) {
    if (depths.has(el))
      return depths.get(el);
    let depth = 0;
    let owner = el;
    while (el) {
      depth++;
      if (el._x_teleportBack) {
        el = el._x_teleportBack;
      } else if (typeof ShadowRoot === "function" && el.parentNode instanceof ShadowRoot) {
        el = el.parentNode.host;
      } else {
        el = el.parentElement;
      }
    }
    depths.set(owner, depth);
    return depth;
  }
  var reactive;
  var effect;
  var release;
  var raw;
  var nextStructuralEffectOrder = 0;
  var shouldSchedule = true;
  function disableEffectScheduling(callback) {
    shouldSchedule = false;
    callback();
    shouldSchedule = true;
  }
  function setReactivityEngine(engine) {
    reactive = engine.reactive;
    release = engine.release;
    effect = (callback) => engine.effect(callback, { scheduler: (task) => {
      if (shouldSchedule) {
        scheduler(task);
      } else {
        task();
      }
    } });
    raw = engine.raw;
  }
  function overrideEffect(override) {
    effect = override;
  }
  function elementBoundEffect(el) {
    let cleanup = () => {
    };
    let wrappedEffect = (callback, options) => {
      let priority = options?.priority === "structural" ? nextStructuralEffectOrder++ : void 0;
      let effectReference = effect(callback);
      if (priority !== void 0 && effectReference !== void 0) {
        effectReference._x_schedulerPriority = { el, order: priority };
      }
      if (!el._x_effects) {
        el._x_effects = /* @__PURE__ */ new Set();
        el._x_runEffects = () => {
          el._x_effects.forEach((i) => i());
        };
      }
      el._x_effects.add(effectReference);
      cleanup = () => {
        if (effectReference === void 0)
          return;
        el._x_effects.delete(effectReference);
        release(effectReference);
      };
      return effectReference;
    };
    return [wrappedEffect, () => {
      cleanup();
    }];
  }
  function watch(getter, callback) {
    let firstTime = true;
    let oldValue;
    let oldValueJSON;
    let effectReference = effect(() => {
      let value = getter();
      let newJSON = JSON.stringify(value);
      if (!firstTime) {
        if (typeof value === "object" || value !== oldValue) {
          let previousValue = typeof oldValue === "object" ? JSON.parse(oldValueJSON) : oldValue;
          queueMicrotask(() => {
            callback(value, previousValue);
          });
        }
      }
      oldValue = value;
      oldValueJSON = newJSON;
      firstTime = false;
    });
    return () => {
      dequeueJob(effectReference);
      release(effectReference);
    };
  }
  async function transaction(callback) {
    startTransaction();
    try {
      await callback();
      await Promise.resolve();
    } finally {
      commitTransaction();
    }
  }
  var onAttributeAddeds = [];
  var onElRemoveds = [];
  var onElAddeds = [];
  function onElAdded(callback) {
    onElAddeds.push(callback);
  }
  function onElRemoved(el, callback) {
    if (typeof callback === "function") {
      if (!el._x_cleanups)
        el._x_cleanups = [];
      el._x_cleanups.push(callback);
    } else {
      callback = el;
      onElRemoveds.push(callback);
    }
  }
  function onAttributesAdded(callback) {
    onAttributeAddeds.push(callback);
  }
  function onAttributeRemoved(el, name, callback) {
    if (!el._x_attributeCleanups)
      el._x_attributeCleanups = {};
    if (!el._x_attributeCleanups[name])
      el._x_attributeCleanups[name] = [];
    el._x_attributeCleanups[name].push(callback);
  }
  function cleanupAttributes(el, names) {
    if (!el._x_attributeCleanups)
      return;
    Object.entries(el._x_attributeCleanups).forEach(([name, value]) => {
      if (names === void 0 || names.includes(name)) {
        value.forEach((i) => i());
        delete el._x_attributeCleanups[name];
      }
    });
  }
  function cleanupElement(el) {
    el._x_effects?.forEach(dequeueJob);
    while (el._x_cleanups?.length)
      el._x_cleanups.pop()();
  }
  var observer = new MutationObserver(onMutate);
  var currentlyObserving = false;
  function startObservingMutations() {
    observer.observe(document, { subtree: true, childList: true, attributes: true, attributeOldValue: true });
    currentlyObserving = true;
  }
  function stopObservingMutations() {
    flushObserver();
    observer.disconnect();
    currentlyObserving = false;
  }
  var queuedMutations = [];
  function flushObserver() {
    let records = observer.takeRecords();
    queuedMutations.push(() => records.length > 0 && onMutate(records));
    let queueLengthWhenTriggered = queuedMutations.length;
    queueMicrotask(() => {
      if (queuedMutations.length === queueLengthWhenTriggered) {
        while (queuedMutations.length > 0)
          queuedMutations.shift()();
      }
    });
  }
  function flushPendingMutations() {
    while (queuedMutations.length > 0)
      queuedMutations.shift()();
    let records = observer.takeRecords();
    if (records.length > 0)
      onMutate(records);
  }
  function mutateDom(callback) {
    if (!currentlyObserving)
      return callback();
    stopObservingMutations();
    try {
      return callback();
    } finally {
      startObservingMutations();
    }
  }
  var isCollecting = false;
  var deferredMutations = [];
  function deferMutations() {
    isCollecting = true;
  }
  function flushAndStopDeferringMutations() {
    isCollecting = false;
    onMutate(deferredMutations);
    deferredMutations = [];
  }
  function onMutate(mutations) {
    if (isCollecting) {
      deferredMutations = deferredMutations.concat(mutations);
      return;
    }
    let addedNodes = [];
    let removedNodes = /* @__PURE__ */ new Set();
    let addedAttributes = /* @__PURE__ */ new Map();
    let removedAttributes = /* @__PURE__ */ new Map();
    for (let i = 0; i < mutations.length; i++) {
      if (mutations[i].target._x_ignoreMutationObserver)
        continue;
      if (mutations[i].type === "childList") {
        mutations[i].removedNodes.forEach((node) => {
          if (node.nodeType !== 1)
            return;
          if (!node._x_marker)
            return;
          removedNodes.add(node);
        });
        mutations[i].addedNodes.forEach((node) => {
          if (node.nodeType !== 1)
            return;
          if (removedNodes.has(node)) {
            removedNodes.delete(node);
            return;
          }
          if (node._x_marker)
            return;
          addedNodes.push(node);
        });
      }
      if (mutations[i].type === "attributes") {
        let el = mutations[i].target;
        let name = mutations[i].attributeName;
        let oldValue = mutations[i].oldValue;
        let add = () => {
          if (!addedAttributes.has(el))
            addedAttributes.set(el, []);
          addedAttributes.get(el).push({ name, value: el.getAttribute(name) });
        };
        let remove2 = () => {
          if (!removedAttributes.has(el))
            removedAttributes.set(el, []);
          removedAttributes.get(el).push(name);
        };
        if (el.hasAttribute(name) && oldValue === null) {
          add();
        } else if (el.hasAttribute(name)) {
          remove2();
          add();
        } else {
          remove2();
        }
      }
    }
    removedAttributes.forEach((attrs, el) => {
      cleanupAttributes(el, attrs);
    });
    addedAttributes.forEach((attrs, el) => {
      onAttributeAddeds.forEach((i) => i(el, attrs));
    });
    for (let node of removedNodes) {
      if (addedNodes.some((i) => i.contains(node)))
        continue;
      onElRemoveds.forEach((i) => i(node));
    }
    for (let node of addedNodes) {
      if (!node.isConnected)
        continue;
      onElAddeds.forEach((i) => i(node));
    }
    addedNodes = null;
    removedNodes = null;
    addedAttributes = null;
    removedAttributes = null;
  }
  function scope(node) {
    return mergeProxies(closestDataStack(node));
  }
  function addScopeToNode(node, data2, referenceNode) {
    node._x_dataStack = [data2, ...closestDataStack(referenceNode || node)];
    return () => {
      node._x_dataStack = node._x_dataStack.filter((i) => i !== data2);
    };
  }
  function closestDataStack(node) {
    if (node._x_dataStack)
      return node._x_dataStack;
    if (typeof ShadowRoot === "function" && node instanceof ShadowRoot) {
      return closestDataStack(node.host);
    }
    if (!node.parentNode) {
      return [];
    }
    return closestDataStack(node.parentNode);
  }
  function mergeProxies(objects) {
    return new Proxy({ objects }, mergeProxyTrap);
  }
  function keyInPrototypeChain(obj, key) {
    if (obj === null || obj === Object.prototype)
      return null;
    if (Object.prototype.hasOwnProperty.call(obj, key))
      return obj;
    return keyInPrototypeChain(Object.getPrototypeOf(obj), key);
  }
  var mergeProxyTrap = {
    ownKeys({ objects }) {
      return Array.from(
        new Set(objects.flatMap((i) => Object.keys(i)))
      );
    },
    has({ objects }, name) {
      if (name == Symbol.unscopables)
        return false;
      return objects.some(
        (obj) => Object.prototype.hasOwnProperty.call(obj, name) || Reflect.has(obj, name)
      );
    },
    get({ objects }, name, thisProxy) {
      if (name == "toJSON")
        return collapseProxies;
      return Reflect.get(
        objects.find(
          (obj) => Reflect.has(obj, name)
        ) || {},
        name,
        thisProxy
      );
    },
    set({ objects }, name, value, thisProxy) {
      let target;
      for (const obj of objects) {
        target = keyInPrototypeChain(obj, name);
        if (target)
          break;
      }
      if (!target)
        target = objects[objects.length - 1];
      const descriptor = Object.getOwnPropertyDescriptor(target, name);
      if (descriptor?.set && descriptor?.get)
        return descriptor.set.call(thisProxy, value) || true;
      return Reflect.set(target, name, value);
    }
  };
  function collapseProxies() {
    let keys = Reflect.ownKeys(this);
    return keys.reduce((acc, key) => {
      acc[key] = Reflect.get(this, key);
      return acc;
    }, {});
  }
  function initInterceptors(data2, cleanup = () => {
  }) {
    let isObject3 = (val) => typeof val === "object" && !Array.isArray(val) && val !== null;
    let recurse = (obj, basePath = "") => {
      Object.entries(Object.getOwnPropertyDescriptors(obj)).forEach(([key, { value, enumerable }]) => {
        if (enumerable === false || value === void 0)
          return;
        if (typeof value === "object" && value !== null && value.__v_skip)
          return;
        let path = basePath === "" ? key : `${basePath}.${key}`;
        if (typeof value === "object" && value !== null && value._x_interceptor) {
          obj[key] = value.initialize(data2, path, key, cleanup);
        } else {
          if (isObject3(value) && value !== obj && !(value instanceof Element)) {
            recurse(value, path);
          }
        }
      });
    };
    return recurse(data2);
  }
  function interceptor(callback, mutateObj = () => {
  }) {
    let obj = {
      initialValue: void 0,
      _x_interceptor: true,
      initialize(data2, path, key, cleanup) {
        return callback(this.initialValue, () => get(data2, path), (value) => set(data2, path, value), path, key, cleanup);
      }
    };
    mutateObj(obj);
    return (initialValue) => {
      if (typeof initialValue === "object" && initialValue !== null && initialValue._x_interceptor) {
        let initialize = obj.initialize.bind(obj);
        obj.initialize = (data2, path, key, cleanup) => {
          let innerValue = initialValue.initialize(data2, path, key, cleanup);
          obj.initialValue = innerValue;
          return initialize(data2, path, key, cleanup);
        };
      } else {
        obj.initialValue = initialValue;
      }
      return obj;
    };
  }
  function get(obj, path) {
    return path.split(".").reduce((carry, segment) => carry[segment], obj);
  }
  function set(obj, path, value) {
    if (typeof path === "string")
      path = path.split(".");
    if (path.length === 1)
      obj[path[0]] = value;
    else if (path.length === 0)
      throw error;
    else {
      if (obj[path[0]])
        return set(obj[path[0]], path.slice(1), value);
      else {
        obj[path[0]] = {};
        return set(obj[path[0]], path.slice(1), value);
      }
    }
  }
  var magics = {};
  function magic(name, callback) {
    magics[name] = callback;
  }
  function injectMagics(obj, el) {
    let memoizedUtilities = getUtilities(el);
    Object.entries(magics).forEach(([name, callback]) => {
      Object.defineProperty(obj, `$${name}`, {
        get() {
          return callback(el, memoizedUtilities);
        },
        enumerable: false
      });
    });
    return obj;
  }
  function getUtilities(el) {
    let [utilities, cleanup] = getElementBoundUtilities(el);
    let utils = { interceptor, ...utilities };
    onElRemoved(el, cleanup);
    return utils;
  }
  function tryCatch(el, expression, callback, ...args) {
    try {
      return callback(...args);
    } catch (e) {
      handleError(e, el, expression);
    }
  }
  function handleError(...args) {
    return errorHandler(...args);
  }
  var errorHandler = normalErrorHandler;
  function setErrorHandler(handler4) {
    errorHandler = handler4;
  }
  function normalErrorHandler(error2, el, expression = void 0) {
    error2 = Object.assign(
      error2 ?? { message: "No error message given." },
      { el, expression }
    );
    console.warn(`Alpine Expression Error: ${error2.message}

${expression ? 'Expression: "' + expression + '"\n\n' : ""}`, el);
    setTimeout(() => {
      throw error2;
    }, 0);
  }
  var shouldAutoEvaluateFunctions = true;
  function dontAutoEvaluateFunctions(callback) {
    let cache = shouldAutoEvaluateFunctions;
    shouldAutoEvaluateFunctions = false;
    let result = callback();
    shouldAutoEvaluateFunctions = cache;
    return result;
  }
  function evaluate(el, expression, extras = {}) {
    let result;
    evaluateLater(el, expression)((value) => result = value, extras);
    return result;
  }
  function evaluateLater(...args) {
    return theEvaluatorFunction(...args);
  }
  var theEvaluatorFunction = () => {
  };
  function setEvaluator(newEvaluator) {
    theEvaluatorFunction = newEvaluator;
  }
  var theRawEvaluatorFunction;
  function setRawEvaluator(newEvaluator) {
    theRawEvaluatorFunction = newEvaluator;
  }
  function normalEvaluator(el, expression) {
    let overriddenMagics = {};
    injectMagics(overriddenMagics, el);
    let dataStack = [overriddenMagics, ...closestDataStack(el)];
    let evaluator = typeof expression === "function" ? generateEvaluatorFromFunction(dataStack, expression) : generateEvaluatorFromString(dataStack, expression, el);
    return tryCatch.bind(null, el, expression, evaluator);
  }
  function generateEvaluatorFromFunction(dataStack, func) {
    return (receiver = () => {
    }, { scope: scope2 = {}, params = [], context } = {}) => {
      if (!shouldAutoEvaluateFunctions) {
        runIfTypeOfFunction(receiver, func, mergeProxies([scope2, ...dataStack]), params);
        return;
      }
      let result = func.apply(mergeProxies([scope2, ...dataStack]), params);
      runIfTypeOfFunction(receiver, result);
    };
  }
  var evaluatorMemo = {};
  function generateFunctionFromString(expression, el) {
    if (evaluatorMemo[expression]) {
      return evaluatorMemo[expression];
    }
    let AsyncFunction = Object.getPrototypeOf(async function() {
    }).constructor;
    let rightSideSafeExpression = /^[\n\s]*if.*\(.*\)/.test(expression.trim()) || /^(let|const)\s/.test(expression.trim()) ? `(async()=>{ ${expression} })()` : expression;
    const safeAsyncFunction = () => {
      try {
        let func2 = new AsyncFunction(
          ["__self", "scope"],
          `with (scope) { __self.result = ${rightSideSafeExpression} }; __self.finished = true; return __self.result;`
        );
        Object.defineProperty(func2, "name", {
          value: `[Alpine] ${expression}`
        });
        return func2;
      } catch (error2) {
        handleError(error2, el, expression);
        return Promise.resolve();
      }
    };
    let func = safeAsyncFunction();
    evaluatorMemo[expression] = func;
    return func;
  }
  function generateEvaluatorFromString(dataStack, expression, el) {
    let func = generateFunctionFromString(expression, el);
    return (receiver = () => {
    }, { scope: scope2 = {}, params = [], context } = {}) => {
      func.result = void 0;
      func.finished = false;
      let completeScope = mergeProxies([scope2, ...dataStack]);
      if (typeof func === "function") {
        let promise = func.call(context, func, completeScope).catch((error2) => handleError(error2, el, expression));
        if (func.finished) {
          runIfTypeOfFunction(receiver, func.result, completeScope, params, el);
          func.result = void 0;
        } else {
          promise.then((result) => {
            runIfTypeOfFunction(receiver, result, completeScope, params, el);
          }).catch((error2) => handleError(error2, el, expression)).finally(() => func.result = void 0);
        }
      }
    };
  }
  function runIfTypeOfFunction(receiver, value, scope2, params, el) {
    if (shouldAutoEvaluateFunctions && typeof value === "function") {
      let result = value.apply(scope2, params);
      if (result instanceof Promise) {
        result.then((i) => runIfTypeOfFunction(receiver, i, scope2, params)).catch((error2) => handleError(error2, el, value));
      } else {
        receiver(result);
      }
    } else if (typeof value === "object" && value instanceof Promise) {
      value.then((i) => receiver(i));
    } else {
      receiver(value);
    }
  }
  function evaluateRaw(...args) {
    return theRawEvaluatorFunction(...args);
  }
  function normalRawEvaluator(el, expression, extras = {}) {
    let overriddenMagics = {};
    injectMagics(overriddenMagics, el);
    let dataStack = [overriddenMagics, ...closestDataStack(el)];
    let scope2 = mergeProxies([extras.scope ?? {}, ...dataStack]);
    let params = extras.params ?? [];
    if (expression.includes("await")) {
      let AsyncFunction = Object.getPrototypeOf(async function() {
      }).constructor;
      let rightSideSafeExpression = /^[\n\s]*if.*\(.*\)/.test(expression.trim()) || /^(let|const)\s/.test(expression.trim()) ? `(async()=>{ ${expression} })()` : expression;
      let func = new AsyncFunction(
        ["scope"],
        `with (scope) { let __result = ${rightSideSafeExpression}; return __result }`
      );
      let result = func.call(extras.context, scope2);
      return result;
    } else {
      let rightSideSafeExpression = /^[\n\s]*if.*\(.*\)/.test(expression.trim()) || /^(let|const)\s/.test(expression.trim()) ? `(()=>{ ${expression} })()` : expression;
      let func = new Function(
        ["scope"],
        `with (scope) { let __result = ${rightSideSafeExpression}; return __result }`
      );
      let result = func.call(extras.context, scope2);
      if (typeof result === "function" && shouldAutoEvaluateFunctions) {
        return result.apply(scope2, params);
      }
      return result;
    }
  }
  var prefixAsString = "x-";
  function prefix(subject = "") {
    return prefixAsString + subject;
  }
  function setPrefix(newPrefix) {
    prefixAsString = newPrefix;
  }
  var directiveHandlers = {};
  function directive(name, callback) {
    directiveHandlers[name] = callback;
    return {
      before(directive2) {
        if (!directiveHandlers[directive2]) {
          console.warn(String.raw`Cannot find directive \`${directive2}\`. \`${name}\` will use the default order of execution`);
          return;
        }
        const pos = directiveOrder.indexOf(directive2);
        directiveOrder.splice(pos >= 0 ? pos : directiveOrder.indexOf("DEFAULT"), 0, name);
      }
    };
  }
  function directiveExists(name) {
    return Object.keys(directiveHandlers).includes(name);
  }
  function directives(el, attributes, originalAttributeOverride) {
    attributes = Array.from(attributes);
    if (el._x_virtualDirectives) {
      let vAttributes = Object.entries(el._x_virtualDirectives).map(([name, value]) => ({ name, value }));
      let staticAttributes = attributesOnly(vAttributes);
      vAttributes = vAttributes.map((attribute) => {
        if (staticAttributes.find((attr) => attr.name === attribute.name)) {
          return {
            name: `x-bind:${attribute.name}`,
            value: `"${attribute.value}"`
          };
        }
        return attribute;
      });
      attributes = attributes.concat(vAttributes);
    }
    let transformedAttributeMap = {};
    let directives2 = attributes.map(toTransformedAttributes((newName, oldName) => transformedAttributeMap[newName] = oldName)).filter(outNonAlpineAttributes).map(toParsedDirectives(transformedAttributeMap, originalAttributeOverride)).sort(byPriority);
    return directives2.map((directive2) => {
      return getDirectiveHandler(el, directive2);
    });
  }
  function attributesOnly(attributes) {
    return Array.from(attributes).map(toTransformedAttributes()).filter((attr) => !outNonAlpineAttributes(attr));
  }
  var isDeferringHandlers = false;
  var directiveHandlerStacks = /* @__PURE__ */ new Map();
  var currentHandlerStackKey = /* @__PURE__ */ Symbol();
  function deferHandlingDirectives(callback) {
    isDeferringHandlers = true;
    let key = /* @__PURE__ */ Symbol();
    currentHandlerStackKey = key;
    directiveHandlerStacks.set(key, []);
    let flushHandlers = () => {
      while (directiveHandlerStacks.get(key).length)
        directiveHandlerStacks.get(key).shift()();
      directiveHandlerStacks.delete(key);
    };
    let stopDeferring = () => {
      isDeferringHandlers = false;
      flushHandlers();
    };
    callback(flushHandlers);
    stopDeferring();
  }
  function getElementBoundUtilities(el) {
    let cleanups = [];
    let cleanup = (callback) => cleanups.push(callback);
    let [effect3, cleanupEffect2] = elementBoundEffect(el);
    cleanups.push(cleanupEffect2);
    let utilities = {
      Alpine: alpine_default,
      effect: effect3,
      cleanup,
      evaluateLater: evaluateLater.bind(evaluateLater, el),
      evaluate: evaluate.bind(evaluate, el)
    };
    let doCleanup = () => cleanups.forEach((i) => i());
    return [utilities, doCleanup];
  }
  function getDirectiveHandler(el, directive2) {
    let noop = () => {
    };
    let handler4 = directiveHandlers[directive2.type] || noop;
    let [utilities, cleanup] = getElementBoundUtilities(el);
    onAttributeRemoved(el, directive2.original, cleanup);
    let fullHandler = () => {
      if (el._x_ignore || el._x_ignoreSelf)
        return;
      handler4.inline && handler4.inline(el, directive2, utilities);
      handler4 = handler4.bind(handler4, el, directive2, utilities);
      isDeferringHandlers ? directiveHandlerStacks.get(currentHandlerStackKey).push(handler4) : handler4();
    };
    fullHandler.runCleanups = cleanup;
    return fullHandler;
  }
  var startingWith = (subject, replacement) => ({ name, value }) => {
    if (name.startsWith(subject))
      name = name.replace(subject, replacement);
    return { name, value };
  };
  var into = (i) => i;
  function toTransformedAttributes(callback = () => {
  }) {
    return ({ name, value }) => {
      let { name: newName, value: newValue } = attributeTransformers.reduce((carry, transform) => {
        return transform(carry);
      }, { name, value });
      if (newName !== name)
        callback(newName, name);
      return { name: newName, value: newValue };
    };
  }
  var attributeTransformers = [];
  function mapAttributes(callback) {
    attributeTransformers.push(callback);
  }
  function outNonAlpineAttributes({ name }) {
    return alpineAttributeRegex().test(name);
  }
  var alpineAttributeRegex = () => new RegExp(`^${prefixAsString}([^:^.]+)\\b`);
  function toParsedDirectives(transformedAttributeMap, originalAttributeOverride) {
    return ({ name, value }) => {
      if (name === value)
        value = "";
      let typeMatch = name.match(alpineAttributeRegex());
      let valueMatch = name.match(/:([a-zA-Z0-9\-_:]+)/);
      let modifiers = name.match(/\.[^.\]]+(?=[^\]]*$)/g) || [];
      let original = originalAttributeOverride || transformedAttributeMap[name] || name;
      return {
        type: typeMatch ? typeMatch[1] : null,
        value: valueMatch ? valueMatch[1] : null,
        modifiers: modifiers.map((i) => i.replace(".", "")),
        expression: value,
        original
      };
    };
  }
  var DEFAULT = "DEFAULT";
  var directiveOrder = [
    "ignore",
    "ref",
    "id",
    "data",
    "anchor",
    "bind",
    "init",
    "for",
    "model",
    "modelable",
    "transition",
    "show",
    "if",
    DEFAULT,
    "teleport"
  ];
  function byPriority(a, b) {
    let typeA = directiveOrder.indexOf(a.type) === -1 ? DEFAULT : a.type;
    let typeB = directiveOrder.indexOf(b.type) === -1 ? DEFAULT : b.type;
    return directiveOrder.indexOf(typeA) - directiveOrder.indexOf(typeB);
  }
  function walk(el, callback) {
    if (typeof ShadowRoot === "function" && el instanceof ShadowRoot) {
      Array.from(el.children).forEach((el2) => walk(el2, callback));
      return;
    }
    let skip = false;
    callback(el, () => skip = true);
    if (skip)
      return;
    let node = el.firstElementChild;
    while (node) {
      walk(node, callback, false);
      node = node.nextElementSibling;
    }
  }
  var isCloning = false;
  function skipDuringClone(callback, fallback = () => {
  }) {
    return (...args) => isCloning ? fallback(...args) : callback(...args);
  }
  function onlyDuringClone(callback) {
    return (...args) => isCloning && callback(...args);
  }
  var interceptors = [];
  function interceptClone(callback) {
    interceptors.push(callback);
  }
  function cloneNode(from, to) {
    interceptors.forEach((i) => i(from, to));
    isCloning = true;
    dontRegisterReactiveSideEffects(() => {
      initTree(to, (el, callback) => {
        callback(el, () => {
        });
      });
    });
    isCloning = false;
  }
  var isCloningLegacy = false;
  function clone(oldEl, newEl) {
    if (!newEl._x_dataStack)
      newEl._x_dataStack = oldEl._x_dataStack;
    isCloning = true;
    isCloningLegacy = true;
    dontRegisterReactiveSideEffects(() => {
      cloneTree(newEl);
    });
    isCloning = false;
    isCloningLegacy = false;
  }
  function cloneTree(el) {
    let hasRunThroughFirstEl = false;
    let shallowWalker = (el2, callback) => {
      walk(el2, (el3, skip) => {
        if (hasRunThroughFirstEl && isRoot(el3))
          return skip();
        hasRunThroughFirstEl = true;
        callback(el3, skip);
      });
    };
    initTree(el, shallowWalker);
  }
  function dontRegisterReactiveSideEffects(callback) {
    let cache = effect;
    overrideEffect((callback2, el) => {
      let storedEffect = cache(callback2);
      release(storedEffect);
      return () => {
      };
    });
    callback();
    overrideEffect(cache);
  }
  var activeDefers = 0;
  function deferInit(el, promise) {
    let record = el._x_deferInit;
    if (!record) {
      record = el._x_deferInit = {
        pending: 0,
        ownsIgnore: !el._x_ignore,
        queuedAttributes: /* @__PURE__ */ new Map()
      };
      if (record.ownsIgnore)
        el._x_ignore = true;
      activeDefers++;
    }
    record.pending++;
    Promise.resolve(promise).catch((error2) => {
      try {
        handleError(error2, el);
      } catch (error3) {
        setTimeout(() => {
          throw error3;
        }, 0);
      }
    }).then(() => settle(el, record));
  }
  function settle(el, record) {
    record.pending--;
    if (record.pending > 0)
      return;
    flushPendingMutations();
    if (record.pending > 0)
      return;
    if (el._x_deferInit !== record)
      return;
    delete el._x_deferInit;
    if (record.ownsIgnore)
      delete el._x_ignore;
    activeDefers--;
    if (!el.isConnected)
      return;
    replayQueuedAttributes(record);
    initTree(el);
  }
  function queueAttributesForDeferredTree(el, attrs) {
    if (activeDefers === 0)
      return false;
    let root = findClosest(el, (i) => i._x_deferInit);
    if (!root)
      return false;
    queueInto(root._x_deferInit, el, attrs.map(({ name }) => name));
    return true;
  }
  function queueInto(record, el, names) {
    let entry = record.queuedAttributes.get(el);
    if (!entry || entry.marker !== el._x_marker) {
      entry = { marker: el._x_marker, names: /* @__PURE__ */ new Set() };
      record.queuedAttributes.set(el, entry);
    }
    names.forEach((name) => entry.names.add(name));
  }
  function replayQueuedAttributes(record) {
    record.queuedAttributes.forEach((entry, el) => {
      if (!el.isConnected)
        return;
      if (!el._x_marker)
        return;
      if (el._x_marker !== entry.marker)
        return;
      let suspendedAncestor = findClosest(el, (i) => i._x_deferInit);
      if (suspendedAncestor) {
        queueInto(suspendedAncestor._x_deferInit, el, Array.from(entry.names));
        return;
      }
      let attrs = Array.from(entry.names).filter((name) => el.hasAttribute(name)).map((name) => ({ name, value: el.getAttribute(name) }));
      if (attrs.length === 0)
        return;
      directives(el, attrs).forEach((handle) => handle());
    });
  }
  interceptClone((from, to) => {
    if (activeDefers === 0)
      return;
    if (!from || from.nodeType !== 1 || !to || to.nodeType !== 1)
      return;
    if (findClosest(from, (i) => i._x_deferInit))
      to._x_ignore = true;
  });
  function dispatch(el, name, detail = {}, options = {}) {
    return el.dispatchEvent(
      new CustomEvent(name, {
        detail,
        bubbles: true,
        // Allows events to pass the shadow DOM barrier.
        composed: true,
        cancelable: true,
        // Allows overriding the default event options.
        ...options
      })
    );
  }
  function warn(message, ...args) {
    console.warn(`Alpine Warning: ${message}`, ...args);
  }
  var started = false;
  function start() {
    if (started)
      warn("Alpine has already been initialized on this page. Calling Alpine.start() more than once can cause problems.");
    started = true;
    if (!document.body)
      warn("Unable to initialize. Trying to load Alpine before `<body>` is available. Did you forget to add `defer` in Alpine's `<script>` tag?");
    dispatch(document, "alpine:init");
    dispatch(document, "alpine:initializing");
    startObservingMutations();
    onElAdded((el) => initTree(el, walk));
    onElRemoved((el) => destroyTree(el));
    onAttributesAdded((el, attrs) => {
      if (queueAttributesForDeferredTree(el, attrs))
        return;
      directives(el, attrs).forEach((handle) => handle());
    });
    let outNestedComponents = (el) => !closestRoot(el.parentElement, true);
    Array.from(document.querySelectorAll(allSelectors().join(","))).filter(outNestedComponents).forEach((el) => {
      initTree(el);
    });
    dispatch(document, "alpine:initialized");
    setTimeout(() => {
      warnAboutMissingPlugins();
    });
  }
  var rootSelectorCallbacks = [];
  var initSelectorCallbacks = [];
  function rootSelectors() {
    return rootSelectorCallbacks.map((fn) => fn());
  }
  function allSelectors() {
    return rootSelectorCallbacks.concat(initSelectorCallbacks).map((fn) => fn());
  }
  function addRootSelector(selectorCallback) {
    rootSelectorCallbacks.push(selectorCallback);
  }
  function addInitSelector(selectorCallback) {
    initSelectorCallbacks.push(selectorCallback);
  }
  function closestRoot(el, includeInitSelectors = false) {
    return findClosest(el, (element) => {
      const selectors = includeInitSelectors ? allSelectors() : rootSelectors();
      if (selectors.some((selector) => element.matches(selector)))
        return true;
    });
  }
  function findClosest(el, callback) {
    if (!el)
      return;
    if (callback(el))
      return el;
    if (el._x_teleportBack)
      return findClosest(el._x_teleportBack, callback);
    if (el.parentNode instanceof ShadowRoot) {
      return findClosest(el.parentNode.host, callback);
    }
    if (!el.parentElement)
      return;
    return findClosest(el.parentElement, callback);
  }
  function isRoot(el) {
    return rootSelectors().some((selector) => el.matches(selector));
  }
  var initInterceptors2 = [];
  function interceptInit(callback) {
    initInterceptors2.push(callback);
  }
  var markerDispenser = 1;
  function initTree(el, walker = walk, intercept = () => {
  }) {
    if (findClosest(el, (i) => i._x_ignore))
      return;
    deferHandlingDirectives(() => {
      walker(el, (el2, skip) => {
        if (el2._x_marker)
          return;
        intercept(el2, skip);
        initInterceptors2.forEach((i) => i(el2, skip));
        directives(el2, el2.attributes).forEach((handle) => handle());
        if (!el2._x_ignore)
          el2._x_marker = markerDispenser++;
        el2._x_ignore && skip();
      });
    });
  }
  function destroyTree(root, walker = walk) {
    walker(root, (el) => {
      cleanupElement(el);
      cleanupAttributes(el);
      delete el._x_marker;
    });
  }
  function warnAboutMissingPlugins() {
    let pluginDirectives = [
      ["ui", "dialog", ["[x-dialog], [x-popover]"]],
      ["anchor", "anchor", ["[x-anchor]"]],
      ["sort", "sort", ["[x-sort]"]]
    ];
    pluginDirectives.forEach(([plugin2, directive2, selectors]) => {
      if (directiveExists(directive2))
        return;
      selectors.some((selector) => {
        if (document.querySelector(selector)) {
          warn(`found "${selector}", but missing ${plugin2} plugin`);
          return true;
        }
      });
    });
  }
  var tickStack = [];
  var isHolding = false;
  function nextTick(callback = () => {
  }) {
    queueMicrotask(() => {
      isHolding || setTimeout(() => {
        releaseNextTicks();
      });
    });
    return new Promise((res) => {
      tickStack.push(() => {
        callback();
        res();
      });
    });
  }
  function releaseNextTicks() {
    isHolding = false;
    while (tickStack.length)
      tickStack.shift()();
  }
  function holdNextTicks() {
    isHolding = true;
  }
  function setClasses(el, value) {
    if (Array.isArray(value)) {
      return setClassesFromString(el, value.join(" "));
    } else if (typeof value === "object" && value !== null) {
      return setClassesFromObject(el, value);
    } else if (typeof value === "function") {
      return setClasses(el, value());
    }
    return setClassesFromString(el, value);
  }
  function splitClasses(classString) {
    return classString.split(/\s/).filter(Boolean);
  }
  function setClassesFromString(el, classString) {
    let missingClasses = (classString2) => splitClasses(classString2).filter((i) => !el.classList.contains(i)).filter(Boolean);
    let addClassesAndReturnUndo = (classes) => {
      el.classList.add(...classes);
      return () => {
        el.classList.remove(...classes);
      };
    };
    classString = classString === true ? classString = "" : classString || "";
    return addClassesAndReturnUndo(missingClasses(classString));
  }
  function setClassesFromObject(el, classObject) {
    let forAdd = Object.entries(classObject).flatMap(([classString, bool]) => bool ? splitClasses(classString) : false).filter(Boolean);
    let forRemove = Object.entries(classObject).flatMap(([classString, bool]) => !bool ? splitClasses(classString) : false).filter(Boolean);
    let added = [];
    let removed = [];
    forRemove.forEach((i) => {
      if (el.classList.contains(i)) {
        el.classList.remove(i);
        removed.push(i);
      }
    });
    forAdd.forEach((i) => {
      if (!el.classList.contains(i)) {
        el.classList.add(i);
        added.push(i);
      }
    });
    return () => {
      removed.forEach((i) => el.classList.add(i));
      added.forEach((i) => el.classList.remove(i));
    };
  }
  function setStyles(el, value) {
    if (typeof value === "object" && value !== null) {
      return setStylesFromObject(el, value);
    }
    return setStylesFromString(el, value);
  }
  function setStylesFromObject(el, value) {
    let previousStyles = {};
    Object.entries(value).forEach(([key, value2]) => {
      previousStyles[key] = el.style[key];
      if (!key.startsWith("--")) {
        key = kebabCase(key);
      }
      el.style.setProperty(key, value2);
    });
    setTimeout(() => {
      if (el.style.length === 0) {
        el.removeAttribute("style");
      }
    });
    return () => {
      setStyles(el, previousStyles);
    };
  }
  function setStylesFromString(el, value) {
    let cache = el.getAttribute("style", value);
    el.setAttribute("style", value);
    return () => {
      el.setAttribute("style", cache || "");
    };
  }
  function kebabCase(subject) {
    return subject.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase();
  }
  function once(callback, fallback = () => {
  }) {
    let called = false;
    return function() {
      if (!called) {
        called = true;
        callback.apply(this, arguments);
      } else {
        fallback.apply(this, arguments);
      }
    };
  }
  directive("transition", (el, { value, modifiers, expression }, { evaluate: evaluate2 }) => {
    if (typeof expression === "function")
      expression = evaluate2(expression);
    if (expression === false)
      return;
    if (!expression || typeof expression === "boolean") {
      registerTransitionsFromHelper(el, modifiers, value);
    } else {
      registerTransitionsFromClassString(el, expression, value);
    }
  });
  function registerTransitionsFromClassString(el, classString, stage) {
    registerTransitionObject(el, setClasses, "");
    let directiveStorageMap = {
      "enter": (classes) => {
        el._x_transition.enter.during = classes;
      },
      "enter-start": (classes) => {
        el._x_transition.enter.start = classes;
      },
      "enter-end": (classes) => {
        el._x_transition.enter.end = classes;
      },
      "leave": (classes) => {
        el._x_transition.leave.during = classes;
      },
      "leave-start": (classes) => {
        el._x_transition.leave.start = classes;
      },
      "leave-end": (classes) => {
        el._x_transition.leave.end = classes;
      }
    };
    directiveStorageMap[stage](classString);
  }
  function registerTransitionsFromHelper(el, modifiers, stage) {
    registerTransitionObject(el, setStyles);
    let doesntSpecify = !modifiers.includes("in") && !modifiers.includes("out") && !stage;
    let transitioningIn = doesntSpecify || modifiers.includes("in") || ["enter"].includes(stage);
    let transitioningOut = doesntSpecify || modifiers.includes("out") || ["leave"].includes(stage);
    if (modifiers.includes("in") && !doesntSpecify) {
      modifiers = modifiers.filter((i, index) => index < modifiers.indexOf("out"));
    }
    if (modifiers.includes("out") && !doesntSpecify) {
      modifiers = modifiers.filter((i, index) => index > modifiers.indexOf("out"));
    }
    let wantsAll = !modifiers.includes("opacity") && !modifiers.includes("scale");
    let wantsOpacity = wantsAll || modifiers.includes("opacity");
    let wantsScale = wantsAll || modifiers.includes("scale");
    let opacityValue = wantsOpacity ? 0 : 1;
    let scaleValue = wantsScale ? modifierValue(modifiers, "scale", 95) / 100 : 1;
    let delay = modifierValue(modifiers, "delay", 0) / 1e3;
    let origin = modifierValue(modifiers, "origin", "center");
    let property = "opacity, transform";
    let durationIn = modifierValue(modifiers, "duration", 150) / 1e3;
    let durationOut = modifierValue(modifiers, "duration", 75) / 1e3;
    let easing = `cubic-bezier(0.4, 0.0, 0.2, 1)`;
    if (transitioningIn) {
      el._x_transition.enter.during = {
        transformOrigin: origin,
        transitionDelay: `${delay}s`,
        transitionProperty: property,
        transitionDuration: `${durationIn}s`,
        transitionTimingFunction: easing
      };
      el._x_transition.enter.start = {
        opacity: opacityValue,
        transform: `scale(${scaleValue})`
      };
      el._x_transition.enter.end = {
        opacity: 1,
        transform: `scale(1)`
      };
    }
    if (transitioningOut) {
      el._x_transition.leave.during = {
        transformOrigin: origin,
        transitionDelay: `${delay}s`,
        transitionProperty: property,
        transitionDuration: `${durationOut}s`,
        transitionTimingFunction: easing
      };
      el._x_transition.leave.start = {
        opacity: 1,
        transform: `scale(1)`
      };
      el._x_transition.leave.end = {
        opacity: opacityValue,
        transform: `scale(${scaleValue})`
      };
    }
  }
  function registerTransitionObject(el, setFunction, defaultValue = {}) {
    if (!el._x_transition)
      el._x_transition = {
        enter: { during: defaultValue, start: defaultValue, end: defaultValue },
        leave: { during: defaultValue, start: defaultValue, end: defaultValue },
        in(before = () => {
        }, after = () => {
        }) {
          transition(el, setFunction, {
            during: this.enter.during,
            start: this.enter.start,
            end: this.enter.end
          }, before, after);
        },
        out(before = () => {
        }, after = () => {
        }) {
          transition(el, setFunction, {
            during: this.leave.during,
            start: this.leave.start,
            end: this.leave.end
          }, before, after);
        }
      };
  }
  window.Element.prototype._x_toggleAndCascadeWithTransitions = function(el, value, show, hide) {
    const nextTick2 = document.visibilityState === "visible" ? requestAnimationFrame : setTimeout;
    let clickAwayCompatibleShow = () => nextTick2(show);
    if (value) {
      if (el._x_transition && (el._x_transition.enter || el._x_transition.leave)) {
        el._x_transition.enter && (Object.entries(el._x_transition.enter.during).length || Object.entries(el._x_transition.enter.start).length || Object.entries(el._x_transition.enter.end).length) ? el._x_transition.in(show) : clickAwayCompatibleShow();
      } else {
        el._x_transition ? el._x_transition.in(show) : clickAwayCompatibleShow();
      }
      return;
    }
    el._x_hidePromise = el._x_transition ? new Promise((resolve, reject) => {
      el._x_transition.out(() => {
      }, () => resolve(hide));
      el._x_transitioning && el._x_transitioning.beforeCancel(() => reject({ isFromCancelledTransition: true }));
    }) : Promise.resolve(hide);
    queueMicrotask(() => {
      let closest = closestHide(el);
      if (closest) {
        if (!closest._x_hideChildren)
          closest._x_hideChildren = [];
        closest._x_hideChildren.push(el);
      } else {
        nextTick2(() => {
          let hideAfterChildren = (el2) => {
            let carry = Promise.all([
              el2._x_hidePromise,
              ...(el2._x_hideChildren || []).map(hideAfterChildren)
            ]).then(([i]) => i?.());
            delete el2._x_hidePromise;
            delete el2._x_hideChildren;
            return carry;
          };
          hideAfterChildren(el).catch((e) => {
            if (!e.isFromCancelledTransition)
              throw e;
          });
        });
      }
    });
  };
  function closestHide(el) {
    let parent = el.parentNode;
    if (!parent)
      return;
    return parent._x_hidePromise ? parent : closestHide(parent);
  }
  function transition(el, setFunction, { during, start: start2, end } = {}, before = () => {
  }, after = () => {
  }) {
    if (el._x_transitioning)
      el._x_transitioning.cancel();
    if (Object.keys(during).length === 0 && Object.keys(start2).length === 0 && Object.keys(end).length === 0) {
      before();
      after();
      return;
    }
    let undoStart, undoDuring, undoEnd;
    performTransition(el, {
      start() {
        undoStart = setFunction(el, start2);
      },
      during() {
        undoDuring = setFunction(el, during);
      },
      before,
      end() {
        undoStart();
        undoEnd = setFunction(el, end);
      },
      after,
      cleanup() {
        undoDuring();
        undoEnd();
      }
    });
  }
  function performTransition(el, stages) {
    let interrupted, reachedBefore, reachedEnd;
    let finish = once(() => {
      mutateDom(() => {
        interrupted = true;
        if (!reachedBefore)
          stages.before();
        if (!reachedEnd) {
          stages.end();
          releaseNextTicks();
        }
        stages.after();
        if (el.isConnected)
          stages.cleanup();
        delete el._x_transitioning;
      });
    });
    el._x_transitioning = {
      beforeCancels: [],
      beforeCancel(callback) {
        this.beforeCancels.push(callback);
      },
      cancel: once(function() {
        while (this.beforeCancels.length) {
          this.beforeCancels.shift()();
        }
        ;
        finish();
      }),
      finish
    };
    mutateDom(() => {
      stages.start();
      stages.during();
    });
    holdNextTicks();
    requestAnimationFrame(() => {
      if (interrupted)
        return;
      let duration = Number(getComputedStyle(el).transitionDuration.replace(/,.*/, "").replace("s", "")) * 1e3;
      let delay = Number(getComputedStyle(el).transitionDelay.replace(/,.*/, "").replace("s", "")) * 1e3;
      if (duration === 0)
        duration = Number(getComputedStyle(el).animationDuration.replace("s", "")) * 1e3;
      mutateDom(() => {
        stages.before();
      });
      reachedBefore = true;
      requestAnimationFrame(() => {
        if (interrupted)
          return;
        mutateDom(() => {
          stages.end();
        });
        releaseNextTicks();
        setTimeout(el._x_transitioning.finish, duration + delay);
        reachedEnd = true;
      });
    });
  }
  function modifierValue(modifiers, key, fallback) {
    if (modifiers.indexOf(key) === -1)
      return fallback;
    const rawValue = modifiers[modifiers.indexOf(key) + 1];
    if (!rawValue)
      return fallback;
    if (key === "scale") {
      if (isNaN(rawValue))
        return fallback;
    }
    if (key === "duration" || key === "delay") {
      let match = rawValue.match(/([0-9]+)ms/);
      if (match)
        return match[1];
    }
    if (key === "origin") {
      if (["top", "right", "left", "center", "bottom"].includes(modifiers[modifiers.indexOf(key) + 2])) {
        return [rawValue, modifiers[modifiers.indexOf(key) + 2]].join(" ");
      }
    }
    return rawValue;
  }
  function bind(el, name, value, modifiers = []) {
    if (!el._x_bindings)
      el._x_bindings = reactive({});
    el._x_bindings[name] = value;
    name = modifiers.includes("camel") ? camelCase(name) : name;
    switch (name) {
      case "value":
        bindInputValue(el, value);
        break;
      case "style":
        bindStyles(el, value);
        break;
      case "class":
        bindClasses(el, value);
        break;
      case "selected":
      case "checked":
        bindAttributeAndProperty(el, name, value);
        break;
      default:
        bindAttribute(el, name, value);
        break;
    }
  }
  function bindInputValue(el, value) {
    if (isRadio(el)) {
      if (el.attributes.value === void 0) {
        el.value = value;
      }
    } else if (isCheckbox(el)) {
      if (Number.isInteger(value)) {
        el.value = value;
      } else if (!Array.isArray(value) && typeof value !== "boolean" && ![null, void 0].includes(value)) {
        el.value = String(value);
      } else {
        if (Array.isArray(value)) {
          el.checked = value.some((val) => checkedAttrLooseCompare(val, el.value));
        } else {
          el.checked = !!value;
        }
      }
    } else if (el.tagName === "SELECT") {
      updateSelect(el, value);
    } else if (el.tagName === "OPTION") {
      bindAttribute(el, "value", value);
    } else {
      if (el.value === value && (typeof value !== "object" || value === null))
        return;
      el.value = value === void 0 ? "" : value;
    }
  }
  function bindClasses(el, value) {
    if (el._x_undoAddedClasses)
      el._x_undoAddedClasses();
    el._x_undoAddedClasses = setClasses(el, value);
  }
  function bindStyles(el, value) {
    if (el._x_undoAddedStyles)
      el._x_undoAddedStyles();
    el._x_undoAddedStyles = setStyles(el, value);
  }
  function bindAttributeAndProperty(el, name, value) {
    bindAttribute(el, name, value);
    setPropertyIfChanged(el, name, value);
  }
  function bindAttribute(el, name, value) {
    if ([null, void 0, false].includes(value) && attributeShouldntBePreservedIfFalsy(name)) {
      el.removeAttribute(name);
    } else {
      if (isBooleanAttr(name))
        value = name;
      if (isObjectAttr(value))
        value = JSON.stringify(value);
      setIfChanged(el, name, value);
    }
  }
  function setIfChanged(el, attrName, value) {
    if (el.getAttribute(attrName) != value) {
      el.setAttribute(attrName, value);
    }
  }
  function setPropertyIfChanged(el, propName, value) {
    if (el[propName] !== value) {
      el[propName] = value;
    }
  }
  function updateSelect(el, value) {
    const arrayWrappedValue = [].concat(value).map((value2) => {
      return value2 + "";
    });
    Array.from(el.options).forEach((option) => {
      option.selected = arrayWrappedValue.includes(option.value);
    });
  }
  function camelCase(subject) {
    return subject.toLowerCase().replace(/-(\w)/g, (match, char) => char.toUpperCase());
  }
  function checkedAttrLooseCompare(valueA, valueB) {
    return valueA == valueB;
  }
  function safeParseBoolean(rawValue) {
    if ([1, "1", "true", "on", "yes", true].includes(rawValue)) {
      return true;
    }
    if ([0, "0", "false", "off", "no", false].includes(rawValue)) {
      return false;
    }
    return rawValue ? Boolean(rawValue) : null;
  }
  var booleanAttributes = /* @__PURE__ */ new Set([
    "allowfullscreen",
    "async",
    "autofocus",
    "autoplay",
    "checked",
    "controls",
    "default",
    "defer",
    "disabled",
    "formnovalidate",
    "inert",
    "ismap",
    "itemscope",
    "loop",
    "multiple",
    "muted",
    "nomodule",
    "novalidate",
    "open",
    "playsinline",
    "readonly",
    "required",
    "reversed",
    "selected",
    "shadowrootclonable",
    "shadowrootdelegatesfocus",
    "shadowrootserializable"
  ]);
  function isBooleanAttr(attrName) {
    return booleanAttributes.has(attrName);
  }
  function attributeShouldntBePreservedIfFalsy(name) {
    return !["aria-pressed", "aria-checked", "aria-expanded", "aria-selected"].includes(name);
  }
  function isObjectAttr(value) {
    return typeof value === "object" && value !== null;
  }
  function getBinding(el, name, fallback) {
    if (el._x_bindings && el._x_bindings[name] !== void 0)
      return el._x_bindings[name];
    return getAttributeBinding(el, name, fallback);
  }
  function extractProp(el, name, fallback, extract = true) {
    if (el._x_bindings && el._x_bindings[name] !== void 0)
      return el._x_bindings[name];
    if (el._x_inlineBindings && el._x_inlineBindings[name] !== void 0) {
      let binding = el._x_inlineBindings[name];
      binding.extract = extract;
      return dontAutoEvaluateFunctions(() => {
        return evaluate(el, binding.expression);
      });
    }
    return getAttributeBinding(el, name, fallback);
  }
  function getAttributeBinding(el, name, fallback) {
    let attr = el.getAttribute(name);
    if (attr === null)
      return typeof fallback === "function" ? fallback() : fallback;
    if (attr === "")
      return true;
    if (isBooleanAttr(name)) {
      return !![name, "true"].includes(attr);
    }
    return attr;
  }
  function isCheckbox(el) {
    return el.type === "checkbox" || el.localName === "ui-checkbox" || el.localName === "ui-switch";
  }
  function isRadio(el) {
    return el.type === "radio" || el.localName === "ui-radio";
  }
  function debounce(func, wait) {
    let timeout;
    return function() {
      const context = this, args = arguments;
      const later = function() {
        timeout = null;
        func.apply(context, args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }
  function throttle(func, limit) {
    let inThrottle;
    return function() {
      let context = this, args = arguments;
      if (!inThrottle) {
        func.apply(context, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  }
  function entangle({ get: outerGet, set: outerSet }, { get: innerGet, set: innerSet }) {
    let firstRun = true;
    let outerHash;
    let innerHash;
    let reference = effect(() => {
      let outer = outerGet();
      let inner = innerGet();
      if (firstRun) {
        innerSet(cloneIfObject(outer));
        firstRun = false;
      } else {
        let outerHashLatest = JSON.stringify(outer);
        let innerHashLatest = JSON.stringify(inner);
        if (outerHashLatest !== outerHash) {
          innerSet(cloneIfObject(outer));
        } else if (outerHashLatest !== innerHashLatest) {
          outerSet(cloneIfObject(inner));
        } else {
        }
      }
      outerHash = JSON.stringify(outerGet());
      innerHash = JSON.stringify(innerGet());
    });
    return () => {
      dequeueJob(reference);
      release(reference);
    };
  }
  function cloneIfObject(value) {
    return typeof value === "object" ? JSON.parse(JSON.stringify(value)) : value;
  }
  function plugin(callback) {
    let callbacks = Array.isArray(callback) ? callback : [callback];
    callbacks.forEach((i) => i(alpine_default));
  }
  var stores = {};
  var isReactive = false;
  function store(name, value) {
    if (!isReactive) {
      stores = reactive(stores);
      isReactive = true;
    }
    if (value === void 0) {
      return stores[name];
    }
    stores[name] = value;
    if (typeof value === "object" && value !== null && value._x_interceptor) {
      stores[name] = value.initialize(stores, name, name, () => {
      });
    } else {
      initInterceptors(stores[name]);
    }
    if (typeof value === "object" && value !== null && value.hasOwnProperty("init") && typeof value.init === "function") {
      stores[name].init();
    }
  }
  function getStores() {
    return stores;
  }
  var binds = {};
  function bind2(name, bindings) {
    let getBindings = typeof bindings !== "function" ? () => bindings : bindings;
    if (name instanceof Element) {
      return applyBindingsObject(name, getBindings());
    } else {
      binds[name] = getBindings;
    }
    return () => {
    };
  }
  function injectBindingProviders(obj) {
    Object.entries(binds).forEach(([name, callback]) => {
      Object.defineProperty(obj, name, {
        get() {
          return (...args) => {
            return callback(...args);
          };
        }
      });
    });
    return obj;
  }
  function applyBindingsObject(el, obj, original) {
    let cleanupRunners = [];
    while (cleanupRunners.length)
      cleanupRunners.pop()();
    let attributes = Object.entries(obj).map(([name, value]) => ({ name, value }));
    let staticAttributes = attributesOnly(attributes);
    attributes = attributes.map((attribute) => {
      if (staticAttributes.find((attr) => attr.name === attribute.name)) {
        return {
          name: `x-bind:${attribute.name}`,
          value: `"${attribute.value}"`
        };
      }
      return attribute;
    });
    directives(el, attributes, original).map((handle) => {
      cleanupRunners.push(handle.runCleanups);
      handle();
    });
    return () => {
      while (cleanupRunners.length)
        cleanupRunners.pop()();
    };
  }
  var datas = {};
  function data(name, callback) {
    datas[name] = callback;
  }
  function injectDataProviders(obj, context) {
    Object.entries(datas).forEach(([name, callback]) => {
      Object.defineProperty(obj, name, {
        get() {
          return (...args) => {
            return callback.bind(context)(...args);
          };
        },
        enumerable: false
      });
    });
    return obj;
  }
  var Alpine = {
    get reactive() {
      return reactive;
    },
    get release() {
      return release;
    },
    get effect() {
      return effect;
    },
    get raw() {
      return raw;
    },
    get transaction() {
      return transaction;
    },
    version: "3.17.4",
    flushAndStopDeferringMutations,
    dontAutoEvaluateFunctions,
    disableEffectScheduling,
    startObservingMutations,
    stopObservingMutations,
    setReactivityEngine,
    onAttributeRemoved,
    onAttributesAdded,
    closestDataStack,
    skipDuringClone,
    onlyDuringClone,
    addRootSelector,
    addInitSelector,
    setErrorHandler,
    interceptClone,
    addScopeToNode,
    deferMutations,
    mapAttributes,
    evaluateLater,
    interceptInit,
    initInterceptors,
    injectMagics,
    setEvaluator,
    setRawEvaluator,
    mergeProxies,
    extractProp,
    findClosest,
    onElRemoved,
    closestRoot,
    destroyTree,
    interceptor,
    // INTERNAL: not public API and is subject to change without major release.
    transition,
    // INTERNAL
    setStyles,
    // INTERNAL
    mutateDom,
    deferInit,
    directive,
    entangle,
    throttle,
    debounce,
    evaluate,
    evaluateRaw,
    initTree,
    nextTick,
    prefixed: prefix,
    prefix: setPrefix,
    plugin,
    magic,
    store,
    start,
    clone,
    // INTERNAL
    cloneNode,
    // INTERNAL
    bound: getBinding,
    $data: scope,
    watch,
    walk,
    data,
    bind: bind2
  };
  var alpine_default = Alpine;
  function makeMap(str) {
    const map = /* @__PURE__ */ Object.create(null);
    for (const key of str.split(","))
      map[key] = 1;
    return (val) => val in map;
  }
  var EMPTY_OBJ = true ? Object.freeze({}) : {};
  var EMPTY_ARR = true ? Object.freeze([]) : [];
  var extend = Object.assign;
  var hasOwnProperty = Object.prototype.hasOwnProperty;
  var hasOwn = (val, key) => hasOwnProperty.call(val, key);
  var isArray = Array.isArray;
  var isMap = (val) => toTypeString(val) === "[object Map]";
  var isString = (val) => typeof val === "string";
  var isSymbol = (val) => typeof val === "symbol";
  var isObject = (val) => val !== null && typeof val === "object";
  var objectToString = Object.prototype.toString;
  var toTypeString = (value) => objectToString.call(value);
  var toRawType = (value) => {
    return toTypeString(value).slice(8, -1);
  };
  var isIntegerKey = (key) => isString(key) && key !== "NaN" && key[0] !== "-" && "" + parseInt(key, 10) === key;
  var cacheStringFunction = (fn) => {
    const cache = /* @__PURE__ */ Object.create(null);
    return (str) => {
      const hit = cache[str];
      return hit || (cache[str] = fn(str));
    };
  };
  var camelizeRE = /-\w/g;
  var camelize = cacheStringFunction(
    (str) => {
      return str.replace(camelizeRE, (c) => c.slice(1).toUpperCase());
    }
  );
  var hyphenateRE = /\B([A-Z])/g;
  var hyphenate = cacheStringFunction(
    (str) => str.replace(hyphenateRE, "-$1").toLowerCase()
  );
  var capitalize = cacheStringFunction((str) => {
    return str.charAt(0).toUpperCase() + str.slice(1);
  });
  var toHandlerKey = cacheStringFunction(
    (str) => {
      const s = str ? `on${capitalize(str)}` : ``;
      return s;
    }
  );
  var hasChanged = (value, oldValue) => !Object.is(value, oldValue);
  var specialBooleanAttrs = `itemscope,allowfullscreen,formnovalidate,ismap,nomodule,novalidate,readonly`;
  var isBooleanAttr2 = /* @__PURE__ */ makeMap(
    specialBooleanAttrs + `,async,autofocus,autoplay,controls,default,defer,disabled,inert,loop,open,required,reversed,scoped,seamless,checked,muted,multiple,selected`
  );
  function warn2(msg, ...args) {
    console.warn(`[Vue warn] ${msg}`, ...args);
  }
  var activeEffectScope;
  var activeSub;
  var pausedQueueEffects = /* @__PURE__ */ new WeakSet();
  var ReactiveEffect = class {
    constructor(fn) {
      this.fn = fn;
      this.deps = void 0;
      this.depsTail = void 0;
      this.flags = 1 | 4;
      this.next = void 0;
      this.cleanup = void 0;
      this.scheduler = void 0;
      if (activeEffectScope) {
        if (activeEffectScope.active) {
          activeEffectScope.effects.push(this);
        } else {
          this.flags &= -2;
        }
      }
    }
    pause() {
      this.flags |= 64;
    }
    resume() {
      if (this.flags & 64) {
        this.flags &= -65;
        if (pausedQueueEffects.has(this)) {
          pausedQueueEffects.delete(this);
          this.trigger();
        }
      }
    }
    /**
     * @internal
     */
    notify() {
      if (this.flags & 2 && !(this.flags & 32)) {
        return;
      }
      if (!(this.flags & 8)) {
        batch(this);
      }
    }
    run() {
      if (!(this.flags & 1)) {
        return this.fn();
      }
      this.flags |= 2;
      cleanupEffect(this);
      prepareDeps(this);
      const prevEffect = activeSub;
      const prevShouldTrack = shouldTrack;
      activeSub = this;
      shouldTrack = true;
      try {
        return this.fn();
      } finally {
        if (activeSub !== this) {
          warn2(
            "Active effect was not restored correctly - this is likely a Vue internal bug."
          );
        }
        cleanupDeps(this);
        activeSub = prevEffect;
        shouldTrack = prevShouldTrack;
        this.flags &= -3;
      }
    }
    stop() {
      if (this.flags & 1) {
        for (let link = this.deps; link; link = link.nextDep) {
          removeSub(link);
        }
        this.deps = this.depsTail = void 0;
        cleanupEffect(this);
        this.onStop && this.onStop();
        this.flags &= -2;
      }
    }
    trigger() {
      if (this.flags & 64) {
        pausedQueueEffects.add(this);
      } else if (this.scheduler) {
        this.scheduler();
      } else {
        this.runIfDirty();
      }
    }
    /**
     * @internal
     */
    runIfDirty() {
      if (isDirty(this)) {
        this.run();
      }
    }
    get dirty() {
      return isDirty(this);
    }
  };
  var batchDepth = 0;
  var batchedSub;
  var batchedComputed;
  function batch(sub, isComputed = false) {
    sub.flags |= 8;
    if (isComputed) {
      sub.next = batchedComputed;
      batchedComputed = sub;
      return;
    }
    sub.next = batchedSub;
    batchedSub = sub;
  }
  function startBatch() {
    batchDepth++;
  }
  function endBatch() {
    if (--batchDepth > 0) {
      return;
    }
    if (batchedComputed) {
      let e = batchedComputed;
      batchedComputed = void 0;
      while (e) {
        const next = e.next;
        e.next = void 0;
        e.flags &= -9;
        e = next;
      }
    }
    let error2;
    while (batchedSub) {
      let e = batchedSub;
      batchedSub = void 0;
      while (e) {
        const next = e.next;
        e.next = void 0;
        e.flags &= -9;
        if (e.flags & 1) {
          try {
            ;
            e.trigger();
          } catch (err) {
            if (!error2)
              error2 = err;
          }
        }
        e = next;
      }
    }
    if (error2)
      throw error2;
  }
  function prepareDeps(sub) {
    for (let link = sub.deps; link; link = link.nextDep) {
      link.version = -1;
      link.prevActiveLink = link.dep.activeLink;
      link.dep.activeLink = link;
    }
  }
  function cleanupDeps(sub) {
    let head;
    let tail = sub.depsTail;
    let link = tail;
    while (link) {
      const prev = link.prevDep;
      if (link.version === -1) {
        if (link === tail)
          tail = prev;
        removeSub(link);
        removeDep(link);
      } else {
        head = link;
      }
      link.dep.activeLink = link.prevActiveLink;
      link.prevActiveLink = void 0;
      link = prev;
    }
    sub.deps = head;
    sub.depsTail = tail;
  }
  function isDirty(sub) {
    for (let link = sub.deps; link; link = link.nextDep) {
      if (link.dep.version !== link.version || link.dep.computed && (refreshComputed(link.dep.computed) || link.dep.version !== link.version)) {
        return true;
      }
    }
    if (sub._dirty) {
      return true;
    }
    return false;
  }
  function refreshComputed(computed) {
    if (computed.flags & 4 && !(computed.flags & 16)) {
      return;
    }
    computed.flags &= -17;
    if (computed.globalVersion === globalVersion) {
      return;
    }
    computed.globalVersion = globalVersion;
    if (!computed.isSSR && computed.flags & 128 && (!computed.deps && !computed._dirty || !isDirty(computed))) {
      return;
    }
    computed.flags |= 2;
    const dep = computed.dep;
    const prevSub = activeSub;
    const prevShouldTrack = shouldTrack;
    activeSub = computed;
    shouldTrack = true;
    try {
      prepareDeps(computed);
      const value = computed.fn(computed._value);
      if (dep.version === 0 || hasChanged(value, computed._value)) {
        computed.flags |= 128;
        computed._value = value;
        dep.version++;
      }
    } catch (err) {
      dep.version++;
      throw err;
    } finally {
      activeSub = prevSub;
      shouldTrack = prevShouldTrack;
      cleanupDeps(computed);
      computed.flags &= -3;
    }
  }
  function removeSub(link, soft = false) {
    const { dep, prevSub, nextSub } = link;
    if (prevSub) {
      prevSub.nextSub = nextSub;
      link.prevSub = void 0;
    }
    if (nextSub) {
      nextSub.prevSub = prevSub;
      link.nextSub = void 0;
    }
    if (dep.subsHead === link) {
      dep.subsHead = nextSub;
    }
    if (dep.subs === link) {
      dep.subs = prevSub;
      if (!prevSub && dep.computed) {
        dep.computed.flags &= -5;
        for (let l = dep.computed.deps; l; l = l.nextDep) {
          removeSub(l, true);
        }
      }
    }
    if (!soft && !--dep.sc && dep.map) {
      dep.map.delete(dep.key);
    }
  }
  function removeDep(link) {
    const { prevDep, nextDep } = link;
    if (prevDep) {
      prevDep.nextDep = nextDep;
      link.prevDep = void 0;
    }
    if (nextDep) {
      nextDep.prevDep = prevDep;
      link.nextDep = void 0;
    }
  }
  function effect2(fn, options) {
    if (fn.effect instanceof ReactiveEffect) {
      fn = fn.effect.fn;
    }
    const e = new ReactiveEffect(fn);
    if (options) {
      extend(e, options);
    }
    try {
      e.run();
    } catch (err) {
      e.stop();
      throw err;
    }
    const runner = e.run.bind(e);
    runner.effect = e;
    return runner;
  }
  function stop(runner) {
    runner.effect.stop();
  }
  var shouldTrack = true;
  var trackStack = [];
  function pauseTracking() {
    trackStack.push(shouldTrack);
    shouldTrack = false;
  }
  function resetTracking() {
    const last = trackStack.pop();
    shouldTrack = last === void 0 ? true : last;
  }
  function cleanupEffect(e) {
    const { cleanup } = e;
    e.cleanup = void 0;
    if (cleanup) {
      const prevSub = activeSub;
      activeSub = void 0;
      try {
        cleanup();
      } finally {
        activeSub = prevSub;
      }
    }
  }
  var globalVersion = 0;
  var Link = class {
    constructor(sub, dep) {
      this.sub = sub;
      this.dep = dep;
      this.version = dep.version;
      this.nextDep = this.prevDep = this.nextSub = this.prevSub = this.prevActiveLink = void 0;
    }
  };
  var Dep = class {
    // TODO isolatedDeclarations "__v_skip"
    constructor(computed) {
      this.computed = computed;
      this.version = 0;
      this.activeLink = void 0;
      this.subs = void 0;
      this.map = void 0;
      this.key = void 0;
      this.sc = 0;
      this.__v_skip = true;
      if (true) {
        this.subsHead = void 0;
      }
    }
    track(debugInfo) {
      if (!activeSub || !shouldTrack || activeSub === this.computed) {
        return;
      }
      let link = this.activeLink;
      if (link === void 0 || link.sub !== activeSub) {
        link = this.activeLink = new Link(activeSub, this);
        if (!activeSub.deps) {
          activeSub.deps = activeSub.depsTail = link;
        } else {
          link.prevDep = activeSub.depsTail;
          activeSub.depsTail.nextDep = link;
          activeSub.depsTail = link;
        }
        addSub(link);
      } else if (link.version === -1) {
        link.version = this.version;
        if (link.nextDep) {
          const next = link.nextDep;
          next.prevDep = link.prevDep;
          if (link.prevDep) {
            link.prevDep.nextDep = next;
          }
          link.prevDep = activeSub.depsTail;
          link.nextDep = void 0;
          activeSub.depsTail.nextDep = link;
          activeSub.depsTail = link;
          if (activeSub.deps === link) {
            activeSub.deps = next;
          }
        }
      }
      if (activeSub.onTrack) {
        activeSub.onTrack(
          extend(
            {
              effect: activeSub
            },
            debugInfo
          )
        );
      }
      return link;
    }
    trigger(debugInfo) {
      this.version++;
      globalVersion++;
      this.notify(debugInfo);
    }
    notify(debugInfo) {
      startBatch();
      try {
        if (true) {
          for (let head = this.subsHead; head; head = head.nextSub) {
            if (head.sub.onTrigger && !(head.sub.flags & 8)) {
              head.sub.onTrigger(
                extend(
                  {
                    effect: head.sub
                  },
                  debugInfo
                )
              );
            }
          }
        }
        for (let link = this.subs; link; link = link.prevSub) {
          if (link.sub.notify()) {
            ;
            link.sub.dep.notify();
          }
        }
      } finally {
        endBatch();
      }
    }
  };
  function addSub(link) {
    link.dep.sc++;
    if (link.sub.flags & 4) {
      const computed = link.dep.computed;
      if (computed && !link.dep.subs) {
        computed.flags |= 4 | 16;
        for (let l = computed.deps; l; l = l.nextDep) {
          addSub(l);
        }
      }
      const currentTail = link.dep.subs;
      if (currentTail !== link) {
        link.prevSub = currentTail;
        if (currentTail)
          currentTail.nextSub = link;
      }
      if (link.dep.subsHead === void 0) {
        link.dep.subsHead = link;
      }
      link.dep.subs = link;
    }
  }
  var targetMap = /* @__PURE__ */ new WeakMap();
  var ITERATE_KEY = /* @__PURE__ */ Symbol(
    true ? "Object iterate" : ""
  );
  var MAP_KEY_ITERATE_KEY = /* @__PURE__ */ Symbol(
    true ? "Map keys iterate" : ""
  );
  var ARRAY_ITERATE_KEY = /* @__PURE__ */ Symbol(
    true ? "Array iterate" : ""
  );
  function track(target, type, key) {
    if (shouldTrack && activeSub) {
      let depsMap = targetMap.get(target);
      if (!depsMap) {
        targetMap.set(target, depsMap = /* @__PURE__ */ new Map());
      }
      let dep = depsMap.get(key);
      if (!dep) {
        depsMap.set(key, dep = new Dep());
        dep.map = depsMap;
        dep.key = key;
      }
      if (true) {
        dep.track({
          target,
          type,
          key
        });
      } else {
        dep.track();
      }
    }
  }
  function trigger(target, type, key, newValue, oldValue, oldTarget) {
    const depsMap = targetMap.get(target);
    if (!depsMap) {
      globalVersion++;
      return;
    }
    const run = (dep) => {
      if (dep) {
        if (true) {
          dep.trigger({
            target,
            type,
            key,
            newValue,
            oldValue,
            oldTarget
          });
        } else {
          dep.trigger();
        }
      }
    };
    startBatch();
    if (type === "clear") {
      depsMap.forEach(run);
    } else {
      const targetIsArray = isArray(target);
      const isArrayIndex = targetIsArray && isIntegerKey(key);
      if (targetIsArray && key === "length") {
        const newLength = Number(newValue);
        depsMap.forEach((dep, key2) => {
          if (key2 === "length" || key2 === ARRAY_ITERATE_KEY || !isSymbol(key2) && key2 >= newLength) {
            run(dep);
          }
        });
      } else {
        if (key !== void 0 || depsMap.has(void 0)) {
          run(depsMap.get(key));
        }
        if (isArrayIndex) {
          run(depsMap.get(ARRAY_ITERATE_KEY));
        }
        switch (type) {
          case "add":
            if (!targetIsArray) {
              run(depsMap.get(ITERATE_KEY));
              if (isMap(target)) {
                run(depsMap.get(MAP_KEY_ITERATE_KEY));
              }
            } else if (isArrayIndex) {
              run(depsMap.get("length"));
            }
            break;
          case "delete":
            if (!targetIsArray) {
              run(depsMap.get(ITERATE_KEY));
              if (isMap(target)) {
                run(depsMap.get(MAP_KEY_ITERATE_KEY));
              }
            }
            break;
          case "set":
            if (isMap(target)) {
              run(depsMap.get(ITERATE_KEY));
            }
            break;
        }
      }
    }
    endBatch();
  }
  function reactiveReadArray(array) {
    const raw2 = toRaw(array);
    if (raw2 === array)
      return raw2;
    track(raw2, "iterate", ARRAY_ITERATE_KEY);
    return isShallow(array) ? raw2 : raw2.map(toReactive);
  }
  function shallowReadArray(arr) {
    track(arr = toRaw(arr), "iterate", ARRAY_ITERATE_KEY);
    return arr;
  }
  function toWrapped(target, item) {
    if (isReadonly(target)) {
      return isReactive2(target) ? toReadonly(toReactive(item)) : toReadonly(item);
    }
    return toReactive(item);
  }
  var arrayInstrumentations = {
    __proto__: null,
    [Symbol.iterator]() {
      return iterator(this, Symbol.iterator, (item) => toWrapped(this, item));
    },
    concat(...args) {
      return reactiveReadArray(this).concat(
        ...args.map((x) => isArray(x) ? reactiveReadArray(x) : x)
      );
    },
    entries() {
      return iterator(this, "entries", (value) => {
        value[1] = toWrapped(this, value[1]);
        return value;
      });
    },
    every(fn, thisArg) {
      return apply(this, "every", fn, thisArg, void 0, arguments);
    },
    filter(fn, thisArg) {
      return apply(
        this,
        "filter",
        fn,
        thisArg,
        (v) => v.map((item) => toWrapped(this, item)),
        arguments
      );
    },
    find(fn, thisArg) {
      return apply(
        this,
        "find",
        fn,
        thisArg,
        (item) => toWrapped(this, item),
        arguments
      );
    },
    findIndex(fn, thisArg) {
      return apply(this, "findIndex", fn, thisArg, void 0, arguments);
    },
    findLast(fn, thisArg) {
      return apply(
        this,
        "findLast",
        fn,
        thisArg,
        (item) => toWrapped(this, item),
        arguments
      );
    },
    findLastIndex(fn, thisArg) {
      return apply(this, "findLastIndex", fn, thisArg, void 0, arguments);
    },
    // flat, flatMap could benefit from ARRAY_ITERATE but are not straight-forward to implement
    forEach(fn, thisArg) {
      return apply(this, "forEach", fn, thisArg, void 0, arguments);
    },
    includes(...args) {
      return searchProxy(this, "includes", args);
    },
    indexOf(...args) {
      return searchProxy(this, "indexOf", args);
    },
    join(separator) {
      return reactiveReadArray(this).join(separator);
    },
    // keys() iterator only reads `length`, no optimization required
    lastIndexOf(...args) {
      return searchProxy(this, "lastIndexOf", args);
    },
    map(fn, thisArg) {
      return apply(this, "map", fn, thisArg, void 0, arguments);
    },
    pop() {
      return noTracking(this, "pop");
    },
    push(...args) {
      return noTracking(this, "push", args);
    },
    reduce(fn, ...args) {
      return reduce(this, "reduce", fn, args);
    },
    reduceRight(fn, ...args) {
      return reduce(this, "reduceRight", fn, args);
    },
    shift() {
      return noTracking(this, "shift");
    },
    // slice could use ARRAY_ITERATE but also seems to beg for range tracking
    some(fn, thisArg) {
      return apply(this, "some", fn, thisArg, void 0, arguments);
    },
    splice(...args) {
      return noTracking(this, "splice", args);
    },
    toReversed() {
      return reactiveReadArray(this).toReversed();
    },
    toSorted(comparer) {
      return reactiveReadArray(this).toSorted(comparer);
    },
    toSpliced(...args) {
      return reactiveReadArray(this).toSpliced(...args);
    },
    unshift(...args) {
      return noTracking(this, "unshift", args);
    },
    values() {
      return iterator(this, "values", (item) => toWrapped(this, item));
    }
  };
  function iterator(self2, method, wrapValue) {
    const arr = shallowReadArray(self2);
    const iter = arr[method]();
    if (arr !== self2 && !isShallow(self2)) {
      iter._next = iter.next;
      iter.next = () => {
        const result = iter._next();
        if (!result.done) {
          result.value = wrapValue(result.value);
        }
        return result;
      };
    }
    return iter;
  }
  var arrayProto = Array.prototype;
  function apply(self2, method, fn, thisArg, wrappedRetFn, args) {
    const arr = shallowReadArray(self2);
    const needsWrap = arr !== self2 && !isShallow(self2);
    const methodFn = arr[method];
    if (methodFn !== arrayProto[method]) {
      const result2 = methodFn.apply(self2, args);
      return needsWrap ? toReactive(result2) : result2;
    }
    let wrappedFn = fn;
    if (arr !== self2) {
      if (needsWrap) {
        wrappedFn = function(item, index) {
          return fn.call(this, toWrapped(self2, item), index, self2);
        };
      } else if (fn.length > 2) {
        wrappedFn = function(item, index) {
          return fn.call(this, item, index, self2);
        };
      }
    }
    const result = methodFn.call(arr, wrappedFn, thisArg);
    return needsWrap && wrappedRetFn ? wrappedRetFn(result) : result;
  }
  function reduce(self2, method, fn, args) {
    const arr = shallowReadArray(self2);
    const needsWrap = arr !== self2 && !isShallow(self2);
    let wrappedFn = fn;
    let wrapInitialAccumulator = false;
    if (arr !== self2) {
      if (needsWrap) {
        wrapInitialAccumulator = args.length === 0;
        wrappedFn = function(acc, item, index) {
          if (wrapInitialAccumulator) {
            wrapInitialAccumulator = false;
            acc = toWrapped(self2, acc);
          }
          return fn.call(this, acc, toWrapped(self2, item), index, self2);
        };
      } else if (fn.length > 3) {
        wrappedFn = function(acc, item, index) {
          return fn.call(this, acc, item, index, self2);
        };
      }
    }
    const result = arr[method](wrappedFn, ...args);
    return wrapInitialAccumulator ? toWrapped(self2, result) : result;
  }
  function searchProxy(self2, method, args) {
    const arr = toRaw(self2);
    track(arr, "iterate", ARRAY_ITERATE_KEY);
    const res = arr[method](...args);
    if ((res === -1 || res === false) && isProxy(args[0])) {
      args[0] = toRaw(args[0]);
      return arr[method](...args);
    }
    return res;
  }
  function noTracking(self2, method, args = []) {
    pauseTracking();
    startBatch();
    const res = toRaw(self2)[method].apply(self2, args);
    endBatch();
    resetTracking();
    return res;
  }
  var isNonTrackableKeys = /* @__PURE__ */ makeMap(`__proto__,__v_isRef,__isVue`);
  var builtInSymbols = new Set(
    /* @__PURE__ */ Object.getOwnPropertyNames(Symbol).filter((key) => key !== "arguments" && key !== "caller").map((key) => Symbol[key]).filter(isSymbol)
  );
  function hasOwnProperty2(key) {
    if (!isSymbol(key))
      key = String(key);
    const obj = toRaw(this);
    track(obj, "has", key);
    return obj.hasOwnProperty(key);
  }
  var BaseReactiveHandler = class {
    constructor(_isReadonly = false, _isShallow = false) {
      this._isReadonly = _isReadonly;
      this._isShallow = _isShallow;
    }
    get(target, key, receiver) {
      if (key === "__v_skip")
        return target["__v_skip"];
      const isReadonly2 = this._isReadonly, isShallow2 = this._isShallow;
      if (key === "__v_isReactive") {
        return !isReadonly2;
      } else if (key === "__v_isReadonly") {
        return isReadonly2;
      } else if (key === "__v_isShallow") {
        return isShallow2;
      } else if (key === "__v_raw") {
        if (receiver === (isReadonly2 ? isShallow2 ? shallowReadonlyMap : readonlyMap : isShallow2 ? shallowReactiveMap : reactiveMap).get(target) || // receiver is not the reactive proxy, but has the same prototype
        // this means the receiver is a user proxy of the reactive proxy
        Object.getPrototypeOf(target) === Object.getPrototypeOf(receiver)) {
          return target;
        }
        return;
      }
      const targetIsArray = isArray(target);
      if (!isReadonly2) {
        let fn;
        if (targetIsArray && (fn = arrayInstrumentations[key])) {
          return fn;
        }
        if (key === "hasOwnProperty") {
          return hasOwnProperty2;
        }
      }
      const res = Reflect.get(
        target,
        key,
        // if this is a proxy wrapping a ref, return methods using the raw ref
        // as receiver so that we don't have to call `toRaw` on the ref in all
        // its class methods
        isRef(target) ? target : receiver
      );
      if (isSymbol(key) ? builtInSymbols.has(key) : isNonTrackableKeys(key)) {
        return res;
      }
      if (!isReadonly2) {
        track(target, "get", key);
      }
      if (isShallow2) {
        return res;
      }
      if (isRef(res)) {
        const value = targetIsArray && isIntegerKey(key) ? res : res.value;
        return isReadonly2 && isObject(value) ? readonly(value) : value;
      }
      if (isObject(res)) {
        return isReadonly2 ? readonly(res) : reactive2(res);
      }
      return res;
    }
  };
  var MutableReactiveHandler = class extends BaseReactiveHandler {
    constructor(isShallow2 = false) {
      super(false, isShallow2);
    }
    set(target, key, value, receiver) {
      let oldValue = target[key];
      const isArrayWithIntegerKey = isArray(target) && isIntegerKey(key);
      if (!this._isShallow) {
        const isOldValueReadonly = isReadonly(oldValue);
        if (!isShallow(value) && !isReadonly(value)) {
          oldValue = toRaw(oldValue);
          value = toRaw(value);
        }
        if (!isArrayWithIntegerKey && isRef(oldValue) && !isRef(value)) {
          if (isOldValueReadonly) {
            if (true) {
              warn2(
                `Set operation on key "${String(key)}" failed: target is readonly.`,
                target[key]
              );
            }
            return true;
          } else {
            oldValue.value = value;
            return true;
          }
        }
      }
      const hadKey = isArrayWithIntegerKey ? Number(key) < target.length : hasOwn(target, key);
      const result = Reflect.set(
        target,
        key,
        value,
        isRef(target) ? target : receiver
      );
      if (target === toRaw(receiver) && result) {
        if (!hadKey) {
          trigger(target, "add", key, value);
        } else if (hasChanged(value, oldValue)) {
          trigger(target, "set", key, value, oldValue);
        }
      }
      return result;
    }
    deleteProperty(target, key) {
      const hadKey = hasOwn(target, key);
      const oldValue = target[key];
      const result = Reflect.deleteProperty(target, key);
      if (result && hadKey) {
        trigger(target, "delete", key, void 0, oldValue);
      }
      return result;
    }
    has(target, key) {
      const result = Reflect.has(target, key);
      if (!isSymbol(key) || !builtInSymbols.has(key)) {
        track(target, "has", key);
      }
      return result;
    }
    ownKeys(target) {
      track(
        target,
        "iterate",
        isArray(target) ? "length" : ITERATE_KEY
      );
      return Reflect.ownKeys(target);
    }
  };
  var ReadonlyReactiveHandler = class extends BaseReactiveHandler {
    constructor(isShallow2 = false) {
      super(true, isShallow2);
    }
    set(target, key) {
      if (true) {
        warn2(
          `Set operation on key "${String(key)}" failed: target is readonly.`,
          target
        );
      }
      return true;
    }
    deleteProperty(target, key) {
      if (true) {
        warn2(
          `Delete operation on key "${String(key)}" failed: target is readonly.`,
          target
        );
      }
      return true;
    }
  };
  var mutableHandlers = /* @__PURE__ */ new MutableReactiveHandler();
  var readonlyHandlers = /* @__PURE__ */ new ReadonlyReactiveHandler();
  var toShallow = (value) => value;
  var getProto = (v) => Reflect.getPrototypeOf(v);
  function createIterableMethod(method, isReadonly2, isShallow2) {
    return function(...args) {
      const target = this["__v_raw"];
      const rawTarget = toRaw(target);
      const targetIsMap = isMap(rawTarget);
      const isPair = method === "entries" || method === Symbol.iterator && targetIsMap;
      const isKeyOnly = method === "keys" && targetIsMap;
      const innerIterator = target[method](...args);
      const wrap = isShallow2 ? toShallow : isReadonly2 ? toReadonly : toReactive;
      !isReadonly2 && track(
        rawTarget,
        "iterate",
        isKeyOnly ? MAP_KEY_ITERATE_KEY : ITERATE_KEY
      );
      return extend(
        // inheriting all iterator properties
        Object.create(innerIterator),
        {
          // iterator protocol
          next() {
            const { value, done } = innerIterator.next();
            return done ? { value, done } : {
              value: isPair ? [wrap(value[0]), wrap(value[1])] : wrap(value),
              done
            };
          }
        }
      );
    };
  }
  function createReadonlyMethod(type) {
    return function(...args) {
      if (true) {
        const key = args[0] ? `on key "${args[0]}" ` : ``;
        warn2(
          `${capitalize(type)} operation ${key}failed: target is readonly.`,
          toRaw(this)
        );
      }
      return type === "delete" ? false : type === "clear" ? void 0 : this;
    };
  }
  function createInstrumentations(readonly2, shallow) {
    const instrumentations = {
      get(key) {
        const target = this["__v_raw"];
        const rawTarget = toRaw(target);
        const rawKey = toRaw(key);
        if (!readonly2) {
          if (hasChanged(key, rawKey)) {
            track(rawTarget, "get", key);
          }
          track(rawTarget, "get", rawKey);
        }
        const { has } = getProto(rawTarget);
        const wrap = shallow ? toShallow : readonly2 ? toReadonly : toReactive;
        if (has.call(rawTarget, key)) {
          return wrap(target.get(key));
        } else if (has.call(rawTarget, rawKey)) {
          return wrap(target.get(rawKey));
        } else if (target !== rawTarget) {
          target.get(key);
        }
      },
      get size() {
        const target = this["__v_raw"];
        !readonly2 && track(toRaw(target), "iterate", ITERATE_KEY);
        return target.size;
      },
      has(key) {
        const target = this["__v_raw"];
        const rawTarget = toRaw(target);
        const rawKey = toRaw(key);
        if (!readonly2) {
          if (hasChanged(key, rawKey)) {
            track(rawTarget, "has", key);
          }
          track(rawTarget, "has", rawKey);
        }
        return key === rawKey ? target.has(key) : target.has(key) || target.has(rawKey);
      },
      forEach(callback, thisArg) {
        const observed = this;
        const target = observed["__v_raw"];
        const rawTarget = toRaw(target);
        const wrap = shallow ? toShallow : readonly2 ? toReadonly : toReactive;
        !readonly2 && track(rawTarget, "iterate", ITERATE_KEY);
        return target.forEach((value, key) => {
          return callback.call(thisArg, wrap(value), wrap(key), observed);
        });
      }
    };
    extend(
      instrumentations,
      readonly2 ? {
        add: createReadonlyMethod("add"),
        set: createReadonlyMethod("set"),
        delete: createReadonlyMethod("delete"),
        clear: createReadonlyMethod("clear")
      } : {
        add(value) {
          const target = toRaw(this);
          const proto = getProto(target);
          const rawValue = toRaw(value);
          const valueToAdd = !shallow && !isShallow(value) && !isReadonly(value) ? rawValue : value;
          const hadKey = proto.has.call(target, valueToAdd) || hasChanged(value, valueToAdd) && proto.has.call(target, value) || hasChanged(rawValue, valueToAdd) && proto.has.call(target, rawValue);
          if (!hadKey) {
            target.add(valueToAdd);
            trigger(target, "add", valueToAdd, valueToAdd);
          }
          return this;
        },
        set(key, value) {
          if (!shallow && !isShallow(value) && !isReadonly(value)) {
            value = toRaw(value);
          }
          const target = toRaw(this);
          const { has, get: get2 } = getProto(target);
          let hadKey = has.call(target, key);
          if (!hadKey) {
            key = toRaw(key);
            hadKey = has.call(target, key);
          } else if (true) {
            checkIdentityKeys(target, has, key);
          }
          const oldValue = get2.call(target, key);
          target.set(key, value);
          if (!hadKey) {
            trigger(target, "add", key, value);
          } else if (hasChanged(value, oldValue)) {
            trigger(target, "set", key, value, oldValue);
          }
          return this;
        },
        delete(key) {
          const target = toRaw(this);
          const { has, get: get2 } = getProto(target);
          let hadKey = has.call(target, key);
          if (!hadKey) {
            key = toRaw(key);
            hadKey = has.call(target, key);
          } else if (true) {
            checkIdentityKeys(target, has, key);
          }
          const oldValue = get2 ? get2.call(target, key) : void 0;
          const result = target.delete(key);
          if (hadKey) {
            trigger(target, "delete", key, void 0, oldValue);
          }
          return result;
        },
        clear() {
          const target = toRaw(this);
          const hadItems = target.size !== 0;
          const oldTarget = true ? isMap(target) ? new Map(target) : new Set(target) : void 0;
          const result = target.clear();
          if (hadItems) {
            trigger(
              target,
              "clear",
              void 0,
              void 0,
              oldTarget
            );
          }
          return result;
        }
      }
    );
    const iteratorMethods = [
      "keys",
      "values",
      "entries",
      Symbol.iterator
    ];
    iteratorMethods.forEach((method) => {
      instrumentations[method] = createIterableMethod(method, readonly2, shallow);
    });
    return instrumentations;
  }
  function createInstrumentationGetter(isReadonly2, shallow) {
    const instrumentations = createInstrumentations(isReadonly2, shallow);
    return (target, key, receiver) => {
      if (key === "__v_isReactive") {
        return !isReadonly2;
      } else if (key === "__v_isReadonly") {
        return isReadonly2;
      } else if (key === "__v_raw") {
        return target;
      }
      return Reflect.get(
        hasOwn(instrumentations, key) && key in target ? instrumentations : target,
        key,
        receiver
      );
    };
  }
  var mutableCollectionHandlers = {
    get: /* @__PURE__ */ createInstrumentationGetter(false, false)
  };
  var readonlyCollectionHandlers = {
    get: /* @__PURE__ */ createInstrumentationGetter(true, false)
  };
  function checkIdentityKeys(target, has, key) {
    const rawKey = toRaw(key);
    if (rawKey !== key && has.call(target, rawKey)) {
      const type = toRawType(target);
      warn2(
        `Reactive ${type} contains both the raw and reactive versions of the same object${type === `Map` ? ` as keys` : ``}, which can lead to inconsistencies. Avoid differentiating between the raw and reactive versions of an object and only use the reactive version if possible.`
      );
    }
  }
  var reactiveMap = /* @__PURE__ */ new WeakMap();
  var shallowReactiveMap = /* @__PURE__ */ new WeakMap();
  var readonlyMap = /* @__PURE__ */ new WeakMap();
  var shallowReadonlyMap = /* @__PURE__ */ new WeakMap();
  function targetTypeMap(rawType) {
    switch (rawType) {
      case "Object":
      case "Array":
        return 1;
      case "Map":
      case "Set":
      case "WeakMap":
      case "WeakSet":
        return 2;
      default:
        return 0;
    }
  }
  function reactive2(target) {
    if (/* @__PURE__ */ isReadonly(target)) {
      return target;
    }
    return createReactiveObject(
      target,
      false,
      mutableHandlers,
      mutableCollectionHandlers,
      reactiveMap
    );
  }
  function readonly(target) {
    return createReactiveObject(
      target,
      true,
      readonlyHandlers,
      readonlyCollectionHandlers,
      readonlyMap
    );
  }
  function createReactiveObject(target, isReadonly2, baseHandlers, collectionHandlers, proxyMap) {
    if (!isObject(target)) {
      if (true) {
        warn2(
          `value cannot be made ${isReadonly2 ? "readonly" : "reactive"}: ${String(
            target
          )}`
        );
      }
      return target;
    }
    if (target["__v_raw"] && !(isReadonly2 && target["__v_isReactive"])) {
      return target;
    }
    if (target["__v_skip"] || !Object.isExtensible(target)) {
      return target;
    }
    const existingProxy = proxyMap.get(target);
    if (existingProxy) {
      return existingProxy;
    }
    const targetType = targetTypeMap(toRawType(target));
    if (targetType === 0) {
      return target;
    }
    const proxy = new Proxy(
      target,
      targetType === 2 ? collectionHandlers : baseHandlers
    );
    proxyMap.set(target, proxy);
    return proxy;
  }
  function isReactive2(value) {
    if (/* @__PURE__ */ isReadonly(value)) {
      return /* @__PURE__ */ isReactive2(value["__v_raw"]);
    }
    return !!(value && value["__v_isReactive"]);
  }
  function isReadonly(value) {
    return !!(value && value["__v_isReadonly"]);
  }
  function isShallow(value) {
    return !!(value && value["__v_isShallow"]);
  }
  function isProxy(value) {
    return value ? !!value["__v_raw"] : false;
  }
  function toRaw(observed) {
    const raw2 = observed && observed["__v_raw"];
    return raw2 ? /* @__PURE__ */ toRaw(raw2) : observed;
  }
  var toReactive = (value) => isObject(value) ? /* @__PURE__ */ reactive2(value) : value;
  var toReadonly = (value) => isObject(value) ? /* @__PURE__ */ readonly(value) : value;
  function isRef(r) {
    return r ? r["__v_isRef"] === true : false;
  }
  magic("nextTick", () => nextTick);
  magic("dispatch", (el) => dispatch.bind(dispatch, el));
  magic("watch", (el, { evaluateLater: evaluateLater2, cleanup }) => (key, callback) => {
    let evaluate2 = evaluateLater2(key);
    let getter = () => {
      let value;
      evaluate2((i) => value = i);
      return value;
    };
    let unwatch = watch(getter, callback);
    cleanup(unwatch);
  });
  magic("store", getStores);
  magic("data", (el) => scope(el));
  magic("root", (el) => closestRoot(el));
  magic("refs", (el) => {
    if (el._x_refs_proxy)
      return el._x_refs_proxy;
    el._x_refs_proxy = mergeProxies(getArrayOfRefObject(el));
    return el._x_refs_proxy;
  });
  function getArrayOfRefObject(el) {
    let refObjects = [];
    findClosest(el, (i) => {
      if (i._x_refs)
        refObjects.push(i._x_refs);
    });
    return refObjects;
  }
  var globalIdMemo = {};
  function findAndIncrementId(name) {
    if (!globalIdMemo[name])
      globalIdMemo[name] = 0;
    return ++globalIdMemo[name];
  }
  function closestIdRoot(el, name) {
    return findClosest(el, (element) => {
      if (element._x_ids && element._x_ids[name])
        return true;
    });
  }
  function setIdRoot(el, name) {
    if (!el._x_ids)
      el._x_ids = {};
    if (!el._x_ids[name])
      el._x_ids[name] = findAndIncrementId(name);
  }
  magic("id", (el, { cleanup }) => (name, key = null) => {
    let cacheKey = `${name}${key ? `-${key}` : ""}`;
    return cacheIdByNameOnElement(el, cacheKey, cleanup, () => {
      let root = closestIdRoot(el, name);
      let id = root ? root._x_ids[name] : findAndIncrementId(name);
      return key ? `${name}-${id}-${key}` : `${name}-${id}`;
    });
  });
  interceptClone((from, to) => {
    if (from._x_id) {
      to._x_id = from._x_id;
    }
  });
  function cacheIdByNameOnElement(el, cacheKey, cleanup, callback) {
    if (!el._x_id)
      el._x_id = {};
    if (el._x_id[cacheKey])
      return el._x_id[cacheKey];
    let output = callback();
    el._x_id[cacheKey] = output;
    cleanup(() => {
      delete el._x_id[cacheKey];
    });
    return output;
  }
  magic("el", (el) => el);
  warnMissingPluginMagic("Focus", "focus", "focus");
  warnMissingPluginMagic("Persist", "persist", "persist");
  function warnMissingPluginMagic(name, magicName, slug) {
    magic(magicName, (el) => warn(`You can't use [$${magicName}] without first installing the "${name}" plugin here: https://alpinejs.dev/plugins/${slug}`, el));
  }
  directive("modelable", (el, { expression }, { effect: effect3, evaluateLater: evaluateLater2, cleanup }) => {
    let func = evaluateLater2(expression);
    let innerGet = () => {
      let result;
      func((i) => result = i);
      return result;
    };
    let evaluateInnerSet = evaluateLater2(`${expression} = __placeholder`);
    let innerSet = (val) => evaluateInnerSet(() => {
    }, { scope: { "__placeholder": val } });
    let initialValue = innerGet();
    innerSet(initialValue);
    queueMicrotask(() => {
      if (!el._x_model)
        return;
      el._x_removeModelListeners["default"]();
      let outerGet = el._x_model.get;
      let outerSet = el._x_model.setWithModifiers;
      let releaseEntanglement = entangle(
        {
          get() {
            return outerGet();
          },
          set(value) {
            outerSet(value);
          }
        },
        {
          get() {
            return innerGet();
          },
          set(value) {
            innerSet(value);
          }
        }
      );
      cleanup(releaseEntanglement);
    });
  });
  directive("teleport", (el, { modifiers, expression }, { cleanup }) => {
    if (el.tagName.toLowerCase() !== "template")
      warn("x-teleport can only be used on a <template> tag", el);
    let target = getTarget(expression);
    let clone2 = el.content.cloneNode(true).firstElementChild;
    el._x_teleport = clone2;
    clone2._x_teleportBack = el;
    el.setAttribute("data-teleport-template", true);
    clone2.setAttribute("data-teleport-target", true);
    if (el._x_forwardEvents) {
      el._x_forwardEvents.forEach((eventName) => {
        clone2.addEventListener(eventName, (e) => {
          e.stopPropagation();
          el.dispatchEvent(new e.constructor(e.type, e));
        });
      });
    }
    addScopeToNode(clone2, {}, el);
    let placeInDom = (clone3, target2, modifiers2) => {
      if (modifiers2.includes("prepend")) {
        target2.parentNode.insertBefore(clone3, target2);
      } else if (modifiers2.includes("append")) {
        target2.parentNode.insertBefore(clone3, target2.nextSibling);
      } else {
        target2.appendChild(clone3);
      }
    };
    mutateDom(() => {
      skipDuringClone(() => {
        placeInDom(clone2, target, modifiers);
        initTree(clone2);
      })();
    });
    el._x_teleportPutBack = () => {
      let target2 = getTarget(expression);
      mutateDom(() => {
        placeInDom(el._x_teleport, target2, modifiers);
      });
    };
    cleanup(
      () => mutateDom(() => {
        clone2.remove();
        destroyTree(clone2);
      })
    );
  });
  var teleportContainerDuringClone = document.createElement("div");
  function getTarget(expression) {
    let target = skipDuringClone(() => {
      return document.querySelector(expression);
    }, () => {
      return teleportContainerDuringClone;
    })();
    if (!target)
      warn(`Cannot find x-teleport element for selector: "${expression}"`);
    return target;
  }
  var handler = () => {
  };
  handler.inline = (el, { modifiers }, { cleanup }) => {
    modifiers.includes("self") ? el._x_ignoreSelf = true : el._x_ignore = true;
    cleanup(() => {
      modifiers.includes("self") ? delete el._x_ignoreSelf : delete el._x_ignore;
    });
  };
  directive("ignore", handler);
  directive("effect", skipDuringClone((el, { expression }, { effect: effect3 }) => {
    effect3(evaluateLater(el, expression));
  }));
  function on(el, event, modifiers, callback) {
    let listenerTarget = el;
    let handler4 = (e) => callback(e);
    let options = {};
    let wrapHandler = (callback2, wrapper) => (e) => wrapper(callback2, e);
    if (modifiers.includes("dot"))
      event = dotSyntax(event);
    if (modifiers.includes("camel"))
      event = camelCase2(event);
    if (modifiers.includes("capture"))
      options.capture = true;
    if (modifiers.includes("window"))
      listenerTarget = window;
    if (modifiers.includes("document"))
      listenerTarget = document;
    if (modifiers.includes("passive")) {
      options.passive = modifiers[modifiers.indexOf("passive") + 1] !== "false";
    }
    handler4 = addDebounceOrThrottle(modifiers, handler4);
    if (modifiers.includes("prevent"))
      handler4 = wrapHandler(handler4, (next, e) => {
        e.preventDefault();
        next(e);
      });
    if (modifiers.includes("stop"))
      handler4 = wrapHandler(handler4, (next, e) => {
        e.stopPropagation();
        next(e);
      });
    if (modifiers.includes("once")) {
      handler4 = wrapHandler(handler4, (next, e) => {
        next(e);
        listenerTarget.removeEventListener(event, handler4, options);
      });
    }
    if (modifiers.includes("away") || modifiers.includes("outside")) {
      listenerTarget = document;
      handler4 = wrapHandler(handler4, (next, e) => {
        if (el.contains(e.target))
          return;
        if (e.target.isConnected === false)
          return;
        if (el.offsetWidth < 1 && el.offsetHeight < 1)
          return;
        if (el._x_isShown === false)
          return;
        next(e);
      });
    }
    if (modifiers.includes("self"))
      handler4 = wrapHandler(handler4, (next, e) => {
        e.target === el && next(e);
      });
    if (event === "submit") {
      handler4 = wrapHandler(handler4, (next, e) => {
        if (e.target._x_pendingModelUpdates) {
          e.target._x_pendingModelUpdates.forEach((fn) => fn());
        }
        next(e);
      });
    }
    if (isKeyEvent(event) || isClickEvent(event)) {
      handler4 = wrapHandler(handler4, (next, e) => {
        if (isListeningForASpecificKeyThatHasntBeenPressed(e, modifiers)) {
          return;
        }
        next(e);
      });
    }
    listenerTarget.addEventListener(event, handler4, options);
    return () => {
      listenerTarget.removeEventListener(event, handler4, options);
    };
  }
  function addDebounceOrThrottle(modifiers, handler4) {
    if (modifiers.includes("debounce")) {
      let nextModifier = modifiers[modifiers.indexOf("debounce") + 1] || "invalid-wait";
      let wait = isNumeric(nextModifier.split("ms")[0]) ? Number(nextModifier.split("ms")[0]) : 250;
      handler4 = debounce(handler4, wait);
    }
    if (modifiers.includes("throttle")) {
      let nextModifier = modifiers[modifiers.indexOf("throttle") + 1] || "invalid-wait";
      let wait = isNumeric(nextModifier.split("ms")[0]) ? Number(nextModifier.split("ms")[0]) : 250;
      handler4 = throttle(handler4, wait);
    }
    return handler4;
  }
  function dotSyntax(subject) {
    return subject.replace(/-/g, ".");
  }
  function camelCase2(subject) {
    return subject.toLowerCase().replace(/-(\w)/g, (match, char) => char.toUpperCase());
  }
  function isNumeric(subject) {
    return !Array.isArray(subject) && !isNaN(subject);
  }
  function kebabCase2(subject) {
    if ([" ", "_"].includes(
      subject
    ))
      return subject;
    return subject.replace(/([a-z])([A-Z])/g, "$1-$2").replace(/[_\s]/, "-").toLowerCase();
  }
  function isKeyEvent(event) {
    return ["keydown", "keyup"].includes(event);
  }
  function isClickEvent(event) {
    return ["contextmenu", "click", "mouse"].some((i) => event.includes(i));
  }
  var nonKeyModifiers = [
    // x-on's own modifiers:
    "window",
    "document",
    "prevent",
    "stop",
    "once",
    "capture",
    "self",
    "away",
    "outside",
    "passive",
    "dot",
    "camel",
    "preserve-scroll",
    // x-model's own modifiers:
    "blur",
    "change",
    "lazy",
    "number",
    "boolean",
    "trim",
    "fill",
    "unintrusive",
    "parent"
  ];
  function isListeningForASpecificKeyThatHasntBeenPressed(e, modifiers) {
    let keyModifiers = modifiers.filter((modifier, index) => {
      if (modifier === "false" && modifiers[index - 1] === "passive")
        return false;
      return !nonKeyModifiers.includes(modifier);
    });
    if (keyModifiers.includes("debounce")) {
      let debounceIndex = keyModifiers.indexOf("debounce");
      keyModifiers.splice(debounceIndex, isNumeric((keyModifiers[debounceIndex + 1] || "invalid-wait").split("ms")[0]) ? 2 : 1);
    }
    if (keyModifiers.includes("throttle")) {
      let debounceIndex = keyModifiers.indexOf("throttle");
      keyModifiers.splice(debounceIndex, isNumeric((keyModifiers[debounceIndex + 1] || "invalid-wait").split("ms")[0]) ? 2 : 1);
    }
    if (keyModifiers.length === 0)
      return false;
    if (keyModifiers.length === 1 && keyToModifiers(e.key).includes(keyModifiers[0]))
      return false;
    const systemKeyModifiers = ["ctrl", "shift", "alt", "meta", "cmd", "super"];
    const selectedSystemKeyModifiers = systemKeyModifiers.filter((modifier) => keyModifiers.includes(modifier));
    keyModifiers = keyModifiers.filter((i) => !selectedSystemKeyModifiers.includes(i));
    if (selectedSystemKeyModifiers.length > 0) {
      const activelyPressedKeyModifiers = selectedSystemKeyModifiers.filter((modifier) => {
        if (modifier === "cmd" || modifier === "super")
          modifier = "meta";
        return e[`${modifier}Key`];
      });
      if (activelyPressedKeyModifiers.length === selectedSystemKeyModifiers.length) {
        if (isClickEvent(e.type))
          return false;
        if (keyToModifiers(e.key).includes(keyModifiers[0]))
          return false;
      }
    }
    return true;
  }
  function keyToModifiers(key) {
    if (!key)
      return [];
    key = kebabCase2(key);
    let modifierToKeyMap = {
      "ctrl": "control",
      "slash": "/",
      "space": " ",
      "spacebar": " ",
      "cmd": "meta",
      "esc": "escape",
      "up": "arrow-up",
      "down": "arrow-down",
      "left": "arrow-left",
      "right": "arrow-right",
      "period": ".",
      "comma": ",",
      "equal": "=",
      "minus": "-",
      "underscore": "_"
    };
    modifierToKeyMap[key] = key;
    return Object.keys(modifierToKeyMap).map((modifier) => {
      if (modifierToKeyMap[modifier] === key)
        return modifier;
    }).filter((modifier) => modifier);
  }
  directive("model", (el, { modifiers, expression }, { effect: effect3, cleanup }) => {
    let scopeTarget = el;
    if (modifiers.includes("parent")) {
      scopeTarget = findClosest(el, (element) => element !== el);
    }
    let evaluateGet = evaluateLater(scopeTarget, expression);
    let evaluateSet;
    if (typeof expression === "string") {
      evaluateSet = evaluateLater(scopeTarget, `${expression} = __placeholder`);
    } else if (typeof expression === "function" && typeof expression() === "string") {
      evaluateSet = evaluateLater(scopeTarget, `${expression()} = __placeholder`);
    } else {
      evaluateSet = () => {
      };
    }
    let getValue = () => {
      let result;
      evaluateGet((value) => result = value);
      return isGetterSetter(result) ? result.get() : result;
    };
    let setValue = (value) => {
      let result;
      evaluateGet((value2) => result = value2);
      if (isGetterSetter(result)) {
        result.set(value);
      } else {
        evaluateSet(() => {
        }, {
          scope: { "__placeholder": value }
        });
      }
    };
    if (typeof expression === "string" && el.type === "radio") {
      mutateDom(() => {
        if (!el.hasAttribute("name"))
          el.setAttribute("name", expression);
      });
    }
    let hasChangeModifier = modifiers.includes("change") || modifiers.includes("lazy");
    let hasBlurModifier = modifiers.includes("blur");
    let hasEnterModifier = modifiers.includes("enter");
    let hasExplicitEventModifiers = hasChangeModifier || hasBlurModifier || hasEnterModifier;
    let removeListener;
    if (isCloning) {
      removeListener = () => {
      };
    } else if (hasExplicitEventModifiers) {
      let listeners = [];
      let syncValue = (e) => setValue(getInputValue(el, modifiers, e, getValue()));
      if (hasChangeModifier) {
        listeners.push(on(el, "change", modifiers, syncValue));
      }
      if (hasBlurModifier) {
        listeners.push(on(el, "blur", modifiers, syncValue));
        if (el.form) {
          let form = el.form;
          let syncCallback = () => syncValue({ target: el });
          if (!form._x_pendingModelUpdates)
            form._x_pendingModelUpdates = [];
          form._x_pendingModelUpdates.push(syncCallback);
          cleanup(() => {
            if (form._x_pendingModelUpdates) {
              form._x_pendingModelUpdates.splice(form._x_pendingModelUpdates.indexOf(syncCallback), 1);
            }
          });
        }
      }
      if (hasEnterModifier) {
        listeners.push(on(el, "keydown", modifiers, (e) => {
          if (e.key === "Enter")
            syncValue(e);
        }));
      }
      removeListener = () => listeners.forEach((remove2) => remove2());
    } else {
      let event = el.tagName.toLowerCase() === "select" || ["checkbox", "radio"].includes(el.type) ? "change" : "input";
      removeListener = on(el, event, modifiers, (e) => {
        setValue(getInputValue(el, modifiers, e, getValue()));
      });
    }
    if (modifiers.includes("fill")) {
      if ([void 0, null, ""].includes(getValue()) || isCheckbox(el) && Array.isArray(getValue()) || el.tagName.toLowerCase() === "select" && el.multiple) {
        setValue(
          getInputValue(el, modifiers, { target: el }, getValue())
        );
      }
    }
    if (!el._x_removeModelListeners)
      el._x_removeModelListeners = {};
    el._x_removeModelListeners["default"] = removeListener;
    cleanup(() => el._x_removeModelListeners["default"]());
    if (el.form) {
      let removeResetListener = on(el.form, "reset", [], (e) => {
        nextTick(() => el._x_model && el._x_model.set(getInputValue(el, modifiers, { target: el }, getValue())));
      });
      cleanup(() => removeResetListener());
    }
    el._x_model = {
      get() {
        return getValue();
      },
      set(value) {
        setValue(value);
      },
      setWithModifiers: addDebounceOrThrottle(modifiers, setValue)
    };
    el._x_forceModelUpdate = (value) => {
      if (value === void 0 && typeof expression === "string" && expression.match(/\./))
        value = "";
      mutateDom(() => {
        if (isCheckbox(el)) {
          if (Array.isArray(value)) {
            el.checked = value.some((val) => val == el.value);
          } else {
            el.checked = !!value;
          }
        } else if (isRadio(el)) {
          if (typeof value === "boolean") {
            el.checked = safeParseBoolean(el.value) === value;
          } else {
            el.checked = el.value == value;
          }
        } else {
          bind(el, "value", value);
        }
      });
    };
    if (el.tagName === "SELECT") {
      let observer2 = new MutationObserver(() => {
        el._x_forceModelUpdate(getValue());
      });
      observer2.observe(el, { childList: true });
      cleanup(() => observer2.disconnect());
    }
    effect3(() => {
      let value = getValue();
      if (modifiers.includes("unintrusive") && document.activeElement.isSameNode(el))
        return;
      el._x_forceModelUpdate(value);
    });
  });
  function getInputValue(el, modifiers, event, currentValue) {
    return mutateDom(() => {
      if (event instanceof CustomEvent && event.detail !== void 0)
        return event.detail !== null && event.detail !== void 0 ? event.detail : event.target.value;
      else if (isCheckbox(el)) {
        if (Array.isArray(currentValue)) {
          let newValue = null;
          if (modifiers.includes("number")) {
            newValue = safeParseNumber(event.target.value);
          } else if (modifiers.includes("boolean")) {
            newValue = safeParseBoolean(event.target.value);
          } else {
            newValue = event.target.value;
          }
          return event.target.checked ? currentValue.includes(newValue) ? currentValue : currentValue.concat([newValue]) : currentValue.filter((el2) => !checkedAttrLooseCompare2(el2, newValue));
        } else {
          return event.target.checked;
        }
      } else if (el.tagName.toLowerCase() === "select" && el.multiple) {
        if (modifiers.includes("number")) {
          return Array.from(event.target.selectedOptions).map((option) => {
            let rawValue = option.value || option.text;
            return safeParseNumber(rawValue);
          });
        } else if (modifiers.includes("boolean")) {
          return Array.from(event.target.selectedOptions).map((option) => {
            let rawValue = option.value || option.text;
            return safeParseBoolean(rawValue);
          });
        }
        return Array.from(event.target.selectedOptions).map((option) => {
          return option.value || option.text;
        });
      } else {
        let newValue;
        if (isRadio(el)) {
          if (event.target.checked) {
            newValue = event.target.value;
          } else {
            newValue = currentValue;
          }
        } else {
          newValue = event.target.value;
        }
        if (modifiers.includes("number")) {
          return safeParseNumber(newValue);
        } else if (modifiers.includes("boolean")) {
          return safeParseBoolean(newValue);
        } else if (modifiers.includes("trim")) {
          return newValue.trim();
        } else {
          return newValue;
        }
      }
    });
  }
  function safeParseNumber(rawValue) {
    let number = rawValue ? parseFloat(rawValue) : null;
    return isNumeric2(number) ? number : rawValue;
  }
  function checkedAttrLooseCompare2(valueA, valueB) {
    return valueA == valueB;
  }
  function isNumeric2(subject) {
    return !Array.isArray(subject) && !isNaN(subject);
  }
  function isGetterSetter(value) {
    return value !== null && typeof value === "object" && typeof value.get === "function" && typeof value.set === "function";
  }
  directive("cloak", (el) => queueMicrotask(() => mutateDom(() => el.removeAttribute(prefix("cloak")))));
  addInitSelector(() => `[${prefix("init")}]`);
  directive("init", skipDuringClone((el, { expression }, { evaluate: evaluate2 }) => {
    if (typeof expression === "string") {
      return !!expression.trim() && evaluate2(expression, {}, false);
    }
    return evaluate2(expression, {}, false);
  }));
  directive("text", (el, { expression }, { effect: effect3, evaluateLater: evaluateLater2 }) => {
    let evaluate2 = evaluateLater2(expression);
    effect3(() => {
      evaluate2((value) => {
        mutateDom(() => {
          el.textContent = value;
        });
      });
    });
  });
  directive("html", (el, { expression }, { effect: effect3, evaluateLater: evaluateLater2 }) => {
    let evaluate2 = evaluateLater2(expression);
    effect3(() => {
      evaluate2((value) => {
        mutateDom(() => {
          Array.from(el.children).forEach((child) => destroyTree(child));
          el.innerHTML = value ?? "";
          el._x_ignoreSelf = true;
          initTree(el);
          delete el._x_ignoreSelf;
        });
      });
    }, { priority: "structural" });
  });
  mapAttributes(startingWith(":", into(prefix("bind:"))));
  var handler2 = (el, { value, modifiers, expression, original }, { effect: effect3, cleanup }) => {
    if (!value) {
      let bindingProviders = {};
      injectBindingProviders(bindingProviders);
      let getBindings = evaluateLater(el, expression);
      getBindings((bindings) => {
        applyBindingsObject(el, bindings, original);
      }, { scope: bindingProviders });
      return;
    }
    if (value === "key")
      return storeKeyForXFor(el, expression);
    if (el._x_inlineBindings && el._x_inlineBindings[value] && el._x_inlineBindings[value].extract) {
      return;
    }
    let evaluate2 = evaluateLater(el, expression);
    effect3(() => evaluate2((result) => {
      if (result === void 0 && typeof expression === "string" && expression.match(/\./)) {
        result = "";
      }
      mutateDom(() => bind(el, value, result, modifiers));
    }));
    cleanup(() => {
      el._x_undoAddedClasses && el._x_undoAddedClasses();
      el._x_undoAddedStyles && el._x_undoAddedStyles();
    });
  };
  handler2.inline = (el, { value, modifiers, expression }) => {
    if (!value)
      return;
    if (!el._x_inlineBindings)
      el._x_inlineBindings = {};
    el._x_inlineBindings[value] = { expression, extract: false };
  };
  directive("bind", handler2);
  function storeKeyForXFor(el, expression) {
    el._x_keyExpression = expression;
  }
  addRootSelector(() => `[${prefix("data")}]`);
  var dataForReconciliation = /* @__PURE__ */ Symbol();
  directive("data", (el, { expression }, { cleanup }) => {
    if (shouldSkipRegisteringDataDuringClone(el))
      return;
    let dataToReconcile = el[dataForReconciliation];
    if (dataToReconcile?.expression === expression)
      return;
    expression = expression === "" ? "{}" : expression;
    let magicContext = {};
    injectMagics(magicContext, el);
    let dataProviderContext = {};
    injectDataProviders(dataProviderContext, magicContext);
    let data2 = evaluate(el, expression, { scope: dataProviderContext });
    if (data2 === void 0 || data2 === true)
      data2 = {};
    injectMagics(data2, el);
    let reactiveData;
    if (dataToReconcile?.reactiveData) {
      reactiveData = dataToReconcile.reactiveData;
      reconcileData(reactiveData, data2);
      let initialized = { expression };
      el[dataForReconciliation] = initialized;
      queueMicrotask(() => {
        if (el[dataForReconciliation] === initialized) {
          delete el[dataForReconciliation];
        }
      });
    } else {
      reactiveData = reactive(data2);
    }
    initInterceptors(reactiveData, cleanup);
    let undo = addScopeToNode(el, reactiveData);
    skipDuringClone(() => {
      reactiveData["init"] && evaluate(el, reactiveData["init"]);
    })();
    cleanup(() => {
      reactiveData["destroy"] && evaluate(el, reactiveData["destroy"]);
      undo();
      let removed = { reactiveData };
      el[dataForReconciliation] = removed;
      queueMicrotask(() => {
        if (el[dataForReconciliation] === removed) {
          delete el[dataForReconciliation];
        }
      });
    });
  });
  function reconcileData(target, source) {
    Object.keys(source).forEach((key) => {
      let descriptor = Object.getOwnPropertyDescriptor(source, key);
      let existingDescriptor = Object.getOwnPropertyDescriptor(target, key);
      if (descriptor.get || descriptor.set || existingDescriptor?.get || existingDescriptor?.set) {
        if (existingDescriptor)
          delete target[key];
        if (!existingDescriptor)
          target[key] = void 0;
        descriptor.get || descriptor.set ? Object.defineProperty(target, key, descriptor) : target[key] = source[key];
      } else {
        target[key] = source[key];
      }
    });
    Object.keys(target).filter((key) => !Object.prototype.hasOwnProperty.call(source, key)).forEach((key) => delete target[key]);
  }
  interceptClone((from, to) => {
    if (from._x_dataStack) {
      to._x_dataStack = from._x_dataStack;
      to.setAttribute("data-has-alpine-state", true);
    }
  });
  function shouldSkipRegisteringDataDuringClone(el) {
    if (!isCloning)
      return false;
    if (isCloningLegacy)
      return true;
    return el.hasAttribute("data-has-alpine-state");
  }
  directive("show", (el, { modifiers, expression }, { effect: effect3 }) => {
    let evaluate2 = evaluateLater(el, expression);
    if (!el._x_doHide)
      el._x_doHide = () => {
        mutateDom(() => {
          el.style.setProperty("display", "none", modifiers.includes("important") ? "important" : void 0);
        });
      };
    if (!el._x_doShow)
      el._x_doShow = () => {
        mutateDom(() => {
          if (el.style.length === 1 && el.style.display === "none") {
            el.removeAttribute("style");
          } else {
            el.style.removeProperty("display");
          }
        });
      };
    let hide = () => {
      el._x_doHide();
      el._x_isShown = false;
    };
    let show = () => {
      el._x_doShow();
      el._x_isShown = true;
    };
    let clickAwayCompatibleShow = () => setTimeout(show);
    let toggle = once(
      (value) => value ? show() : hide(),
      (value) => {
        if (typeof el._x_toggleAndCascadeWithTransitions === "function") {
          el._x_toggleAndCascadeWithTransitions(el, value, show, hide);
        } else {
          value ? clickAwayCompatibleShow() : hide();
        }
      }
    );
    let oldValue;
    let firstTime = true;
    effect3(() => evaluate2((value) => {
      if (!firstTime && value === oldValue)
        return;
      if (modifiers.includes("immediate"))
        value ? clickAwayCompatibleShow() : hide();
      toggle(value);
      oldValue = value;
      firstTime = false;
    }));
  });
  directive("for", skipDuringClone((el, { expression }, { effect: effect3, cleanup }) => {
    let iteratorNames = parseForExpression(expression);
    let evaluateItems = evaluateLater(el, iteratorNames.items);
    let evaluateKey = evaluateLater(
      el,
      // the x-bind:key expression is stored for our use instead of evaluated.
      el._x_keyExpression || "index"
    );
    el._x_lookup = /* @__PURE__ */ new Map();
    effect3(() => loop(el, iteratorNames, evaluateItems, evaluateKey), { priority: "structural" });
    cleanup(() => {
      el._x_lookup.forEach(
        (el2) => mutateDom(() => {
          destroyTree(el2);
          el2.remove();
        })
      );
      delete el._x_lookup;
      delete el._x_lastRenderedEl;
    });
  }));
  function refreshScope(scope2) {
    return (newScope) => {
      Object.entries(newScope).forEach(([key, value]) => {
        scope2[key] = value;
      });
    };
  }
  function loop(templateEl, iteratorNames, evaluateItems, evaluateKey) {
    evaluateItems((items) => {
      if (isNumeric3(items))
        items = Array.from({ length: items }, (_, i) => i + 1);
      if (items === void 0 || items === null)
        items = [];
      if (items instanceof Set)
        items = Array.from(items);
      if (items instanceof Map)
        items = Array.from(items);
      let oldLookup = templateEl._x_lookup;
      let lookup = /* @__PURE__ */ new Map();
      templateEl._x_lookup = lookup;
      let hasStringKeys = isObject2(items);
      let scopeEntries = Object.entries(items).map(([index, item]) => {
        if (!hasStringKeys)
          index = parseInt(index);
        let scope2 = getIterationScopeVariables(iteratorNames, item, index, items);
        let key;
        evaluateKey((innerKey) => {
          if (typeof innerKey === "object")
            warn("x-for key cannot be an object, it must be a string or an integer", templateEl);
          if (oldLookup.has(innerKey)) {
            lookup.set(innerKey, oldLookup.get(innerKey));
            oldLookup.delete(innerKey);
          }
          key = innerKey;
        }, { scope: { index, ...scope2 } });
        return [key, scope2];
      });
      mutateDom(() => {
        oldLookup.forEach((el) => {
          destroyTree(el);
          el.remove();
        });
        let added = /* @__PURE__ */ new Set();
        let prev = templateEl;
        scopeEntries.forEach(([key, scope2]) => {
          if (lookup.has(key)) {
            let el = lookup.get(key);
            el._x_refreshXForScope(scope2);
            if (prev.nextElementSibling !== el) {
              if (prev.nextElementSibling)
                el.replaceWith(prev.nextElementSibling);
              prev.after(el);
            }
            prev = el;
            if (el._x_currentIfEl) {
              if (el.nextElementSibling !== el._x_currentIfEl)
                prev.after(el._x_currentIfEl);
              prev = el._x_currentIfEl;
            }
            return;
          }
          if (templateEl.content.children.length > 1)
            warn("x-for templates require a single root element, additional elements will be ignored.", templateEl);
          let clone2 = document.importNode(templateEl.content, true).firstElementChild;
          let reactiveScope = reactive(scope2);
          addScopeToNode(clone2, reactiveScope, templateEl);
          clone2._x_refreshXForScope = refreshScope(reactiveScope);
          lookup.set(key, clone2);
          added.add(clone2);
          prev.after(clone2);
          prev = clone2;
        });
        added.forEach((clone2) => initTree(clone2));
        if (prev !== templateEl) {
          templateEl._x_lastRenderedEl = prev;
        } else {
          delete templateEl._x_lastRenderedEl;
        }
      });
    });
  }
  function parseForExpression(expression) {
    let forIteratorRE = /,([^,\}\]]*)(?:,([^,\}\]]*))?$/;
    let stripParensRE = /^\s*\(|\)\s*$/g;
    let forAliasRE = /([\s\S]*?)\b(?:in|of)\b([\s\S]*)/;
    let inMatch = expression.match(forAliasRE);
    if (!inMatch)
      return;
    let res = {};
    res.items = inMatch[2].trim();
    let item = inMatch[1].replace(stripParensRE, "").trim();
    let iteratorMatch = item.match(forIteratorRE);
    if (iteratorMatch) {
      res.item = item.replace(forIteratorRE, "").trim();
      res.index = iteratorMatch[1].trim();
      if (iteratorMatch[2]) {
        res.collection = iteratorMatch[2].trim();
      }
    } else {
      res.item = item;
    }
    return res;
  }
  function getIterationScopeVariables(iteratorNames, item, index, items) {
    let scopeVariables = {};
    if (/^\[.*\]$/.test(iteratorNames.item) && Array.isArray(item)) {
      let names = iteratorNames.item.replace("[", "").replace("]", "").split(",").map((i) => i.trim());
      names.forEach((name, i) => {
        scopeVariables[name] = item[i];
      });
    } else if (/^\{.*\}$/.test(iteratorNames.item) && !Array.isArray(item) && typeof item === "object") {
      let names = iteratorNames.item.replace("{", "").replace("}", "").split(",").map((i) => i.trim());
      names.forEach((name) => {
        scopeVariables[name] = item[name];
      });
    } else {
      scopeVariables[iteratorNames.item] = item;
    }
    if (iteratorNames.index)
      scopeVariables[iteratorNames.index] = index;
    if (iteratorNames.collection)
      scopeVariables[iteratorNames.collection] = items;
    return scopeVariables;
  }
  function isNumeric3(subject) {
    return typeof subject !== "object" && !isNaN(subject);
  }
  function isObject2(subject) {
    return typeof subject === "object" && !Array.isArray(subject);
  }
  function handler3() {
  }
  handler3.inline = (el, { expression }, { cleanup }) => {
    let root = closestRoot(el);
    if (!root)
      return;
    if (!root._x_refs)
      root._x_refs = {};
    root._x_refs[expression] = el;
    cleanup(() => delete root._x_refs[expression]);
  };
  directive("ref", handler3);
  directive("if", skipDuringClone((el, { expression }, { effect: effect3, cleanup }) => {
    if (el.tagName.toLowerCase() !== "template")
      warn("x-if can only be used on a <template> tag", el);
    let evaluate2 = evaluateLater(el, expression);
    let show = () => {
      if (el._x_currentIfEl)
        return el._x_currentIfEl;
      let clone2 = el.content.cloneNode(true).firstElementChild;
      addScopeToNode(clone2, {}, el);
      mutateDom(() => {
        el.after(clone2);
        initTree(clone2);
      });
      el._x_currentIfEl = clone2;
      el._x_lastRenderedEl = clone2;
      el._x_undoIf = () => {
        mutateDom(() => {
          destroyTree(clone2);
          clone2.remove();
        });
        delete el._x_currentIfEl;
        delete el._x_lastRenderedEl;
      };
      return clone2;
    };
    let hide = () => {
      if (!el._x_undoIf)
        return;
      el._x_undoIf();
      delete el._x_undoIf;
    };
    effect3(() => evaluate2((value) => {
      value ? show() : hide();
    }), { priority: "structural" });
    cleanup(() => el._x_undoIf && el._x_undoIf());
  }));
  directive("id", (el, { expression }, { evaluate: evaluate2 }) => {
    let names = evaluate2(expression);
    names.forEach((name) => setIdRoot(el, name));
  });
  interceptClone((from, to) => {
    if (from._x_ids) {
      to._x_ids = from._x_ids;
    }
  });
  mapAttributes(startingWith("@", into(prefix("on:"))));
  directive("on", skipDuringClone((el, { value, modifiers, expression }, { cleanup }) => {
    let evaluate2 = expression ? evaluateLater(el, expression) : () => {
    };
    if (el.tagName.toLowerCase() === "template") {
      if (!el._x_forwardEvents)
        el._x_forwardEvents = [];
      if (!el._x_forwardEvents.includes(value))
        el._x_forwardEvents.push(value);
    }
    let removeListener = on(el, value, modifiers, (e) => {
      evaluate2(() => {
      }, { scope: { "$event": e }, params: [e] });
    });
    cleanup(() => removeListener());
  }));
  warnMissingPluginDirective("Collapse", "collapse", "collapse");
  warnMissingPluginDirective("Intersect", "intersect", "intersect");
  warnMissingPluginDirective("Focus", "trap", "focus");
  warnMissingPluginDirective("Mask", "mask", "mask");
  function warnMissingPluginDirective(name, directiveName, slug) {
    directive(directiveName, (el) => warn(`You can't use [x-${directiveName}] without first installing the "${name}" plugin here: https://alpinejs.dev/plugins/${slug}`, el));
  }
  alpine_default.setEvaluator(normalEvaluator);
  alpine_default.setRawEvaluator(normalRawEvaluator);
  alpine_default.setReactivityEngine({
    reactive: reactive2,
    // Since Vue 3.2, the scheduler is called with no arguments, so we wrap
    // the effect to hand Alpine's scheduler the runner it expects to queue.
    effect: (callback, options = {}) => {
      let runner;
      runner = effect2(callback, {
        scheduler: () => {
          if (!runner)
            return;
          options.scheduler ? options.scheduler(runner) : runner();
        }
      });
      return runner;
    },
    release: stop,
    raw: toRaw
  });
  var src_default = alpine_default;
  var module_default = src_default;

  // main.ts
  var sessionId = crypto.randomUUID();
  window.Alpine = module_default;
  window.handleHeartbeat = handleHeartbeat;
  module_default.start();
  document.addEventListener("htmx:sse:before:connection", handleBeforeConnection);
  document.addEventListener("htmx:sse:after:connection", handleAfterConnection);
  document.addEventListener("htmx:sse:before:message", handleBeforeMessage);
  document.addEventListener("htmx:sse:after:message", handleAfterMessage);
  document.addEventListener("htmx:sse:close", handleClose);
  document.addEventListener("htmx:sse:error", handleError2);
  function handleBeforeConnection(event) {
    console.log(event.detail);
  }
  function handleAfterConnection(event) {
    console.log(event.detail);
  }
  function handleBeforeMessage(event) {
    console.log(event.detail);
  }
  function handleAfterMessage(event) {
    console.log(event.detail);
  }
  function handleClose(event) {
    console.log(event.detail);
  }
  function handleError2(event) {
    console.log(event.detail);
  }
  function handleConfigRequest(event) {
    event.detail.ctx.request.headers["X-Session-Id"] = sessionId;
  }
  document.addEventListener("htmx:config:request", handleConfigRequest);
  function handleHeartbeat(event) {
    console.log(event);
  }
})();
/*! Bundled license information:

alpinejs/dist/module.esm.js:
  (*! Bundled license information:
  
  @vue/shared/dist/shared.esm-bundler.js:
    (**
    * @vue/shared v3.5.41
    * (c) 2018-present Yuxi (Evan) You and Vue contributors
    * @license MIT
    **)
  
  @vue/reactivity/dist/reactivity.esm-bundler.js:
    (**
    * @vue/reactivity v3.5.41
    * (c) 2018-present Yuxi (Evan) You and Vue contributors
    * @license MIT
    **)
  *)
*/
//# sourceMappingURL=main.js.map
