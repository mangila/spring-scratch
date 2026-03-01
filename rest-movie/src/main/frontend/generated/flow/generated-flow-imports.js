import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/markdown/src/vaadin-markdown.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '11d30de23330a701115660bef2600caececd430854ba2e5f2b090fd5103a45a0') {
    pending.push(import('./chunks/chunk-52accf672413b176e16b1f7b392f040169ceb537093fde51df4dcadb46d9a97a.js'));
  }
  if (key === 'ffd774c25f7c5e55e69be314e132520923a0bce957fb597d0f2e2099ce57e4c4') {
    pending.push(import('./chunks/chunk-52accf672413b176e16b1f7b392f040169ceb537093fde51df4dcadb46d9a97a.js'));
  }
  if (key === '60f50dc7bff9a05486241d1ab16bd7f84d9f7179fca224f2cbf49d2909ee2537') {
    pending.push(import('./chunks/chunk-b46e697250f8c4e96c821541ab07ecdb64ae8d3c12204bee8c86c514f0cf4e8d.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}