/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
  "use strict";

  window.addEventListener("load", onWindowLoad);

  function onWindowLoad() {
    addTabs();
    configureTabs();
  }

  function addTabs() {
    for (const primaryElement of document.querySelectorAll(".primary")) {
      if (primaryElement.querySelector("div.switch")) {
        console.debug("Skipping tabs due to existing blockswitches");
        return;
      }
      const tabsElement = createTabsElement(primaryElement);
      const tab = createTab(primaryElement, tabsElement);
      tab.tabElement.classList.add("selected");
      primaryElement.querySelector(".title").remove();
      primaryElement.classList.add("tabs-content");
    }
    for (const secondaryElement of document.querySelectorAll(".secondary")) {
      const primaryElement = findPrimaryElement(secondaryElement);
      if (primaryElement) {
        const tabsElement = primaryElement.querySelector(".tabs");
        const tab = createTab(secondaryElement, tabsElement);
        tab.content.classList.add("hidden");
        primaryElement.append(tab.content);
        secondaryElement.remove();
      } else {
        console.error("Found secondary block with no primary sibling");
      }
    }
  }

  function createTabsElement(primaryElement) {
    const tabsElement = createElementFromHtml('<div class="tabs"></div>');
    primaryElement.prepend(tabsElement);
    return tabsElement;
  }

  function createTab(blockElement, tabsElement) {
    const title = blockElement.querySelector(".title").textContent;
    const content = blockElement.querySelectorAll(".content").item(0);
    const colist = nextSibling(blockElement, ".colist");
    if (colist) {
      content.append(colist);
    }
    const tabElement = createElementFromHtml(
      '<div class="tab">' + title + "</div>"
    );
    tabElement.dataset.blockName = title;
    content.dataset.blockName = title;
    tabsElement.append(tabElement);
    return { tabElement: tabElement, content: content };
  }

  function nextSibling(element, selector) {
    let sibling = element.nextElementSibling;
    while (sibling) {
      if (sibling.matches(selector)) {
        return sibling;
      }
      sibling = sibling.nextElementSibling;
    }
  }

  function createElementFromHtml(html) {
    const template = document.createElement("template");
    template.innerHTML = html;
    return template.content.firstChild;
  }

  function findPrimaryElement(secondaryElement) {
    let candidate = secondaryElement.previousElementSibling;
    while (candidate && !candidate.classList.contains("primary")) {
      candidate = candidate.previousElementSibling;
    }
    return candidate;
  }

  function configureTabs() {
    for (const tabElement of document.querySelectorAll(".tab")) {
      const tabId = getTabId(tabElement);
      tabElement.addEventListener("click", onTabClick.bind(tabElement, tabId));
      if (tabElement.textContent === window.localStorage.getItem(tabId)) {
        select(tabElement);
      }
    }
  }

  function onTabClick(tabId) {
    const title = this.textContent;
    window.localStorage.setItem(tabId, title);
    for (const tabElement of document.querySelectorAll(".tab")) {
      if (getTabId(tabElement) === tabId && tabElement.textContent === title) {
        select(tabElement);
      }
    }
  }

  function select(tabElement) {
    for (const child of tabElement.parentNode.children) {
      child.classList.remove("selected");
    }
    tabElement.classList.add("selected");
    for (const child of tabElement.parentNode.parentNode.children) {
      if (child.classList.contains("content")) {
        if (tabElement.dataset.blockName === child.dataset.blockName) {
          child.classList.remove("hidden");
        } else {
          child.classList.add("hidden");
        }
      }
    }
  }

  function getTabId(tabElement) {
    const id = [];
    for (tabElement of tabElement.parentNode.querySelectorAll(".tab")) {
      id.push(tabElement.textContent.toLowerCase());
    }
    return id.sort().join("-");
  }
})();