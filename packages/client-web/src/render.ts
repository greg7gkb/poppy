import type { Action, Button, Component, Image, PoppyDocument, Stack, Text } from "@poppy/schema";
import { validate } from "@poppy/server-ts";
import { isUrlAllowedDefault } from "./sanitize.js";

export interface PoppyHost {
  /** Called when a Button (or future actionable component) fires. Action is forwarded verbatim. */
  onAction: (action: Action) => void;
  /** Validate the document before rendering. Default: true. */
  validate?: boolean;
  /** Override the default URL allowlist for `<img>` sources. */
  isUrlAllowed?: (url: string, context: "image") => boolean;
  /** Receives validation failures and rejected URLs. Default: console.error. */
  onError?: (err: Error) => void;
}

export interface RenderResult {
  /** Replace the current tree with a new document. Same host, same container. */
  update: (doc: unknown) => void;
  /** Remove all event listeners and clear the container. Idempotent. */
  destroy: () => void;
}

/**
 * Render a Poppy document into a container.
 *
 * Validation runs by default; failures are reported through `host.onError`
 * and result in an empty container. Set `host.validate: false` to skip
 * (e.g. when the document was validated server-side and you trust the source).
 */
export function render(document_: unknown, container: HTMLElement, host: PoppyHost): RenderResult {
  let controller = new AbortController();

  const doRender = (input: unknown) => {
    controller.abort();
    controller = new AbortController();
    container.replaceChildren();

    const doc = prepare(input, host);
    if (!doc) return;

    const root = renderComponent(doc.root, host, controller.signal);
    container.appendChild(root);
  };

  doRender(document_);

  return {
    update(doc) {
      doRender(doc);
    },
    destroy() {
      controller.abort();
      container.replaceChildren();
    },
  };
}

function prepare(input: unknown, host: PoppyHost): PoppyDocument | null {
  const shouldValidate = host.validate !== false;
  if (!shouldValidate) {
    return input as PoppyDocument;
  }
  const result = validate(input);
  if (!result.ok) {
    reportError(host, new Error(`Poppy: invalid document — ${formatErrors(result.errors)}`));
    return null;
  }
  return result.document;
}

function formatErrors(errors: { path: string; keyword: string; message: string }[]): string {
  return errors
    .slice(0, 3)
    .map((e) => `${e.path} (${e.keyword}): ${e.message}`)
    .join("; ");
}

function reportError(host: PoppyHost, err: Error): void {
  if (host.onError) host.onError(err);
  else console.error(err);
}

// --- Component dispatch ---------------------------------------------------

function renderComponent(component: Component, host: PoppyHost, signal: AbortSignal): HTMLElement {
  switch (component.type) {
    case "Stack":
      return renderStack(component, host, signal);
    case "Text":
      return renderText(component);
    case "Image":
      return renderImage(component, host);
    case "Button":
      return renderButton(component, host, signal);
  }
}

function renderStack(stack: Stack, host: PoppyHost, signal: AbortSignal): HTMLElement {
  const el = document.createElement("div");
  el.dataset.poppyStack = "";
  el.dataset.axis = stack.axis;
  if (stack.id) el.id = stack.id;

  const classes = ["poppy-stack", `poppy-stack--${stack.axis}`];
  if (stack.spacing) classes.push(`poppy-stack--spacing-${stack.spacing}`);
  if (stack.padding) classes.push(`poppy-stack--padding-${stack.padding}`);
  if (stack.alignment) classes.push(`poppy-stack--align-${stack.alignment}`);
  el.className = classes.join(" ");

  for (const child of stack.children) {
    el.appendChild(renderComponent(child, host, signal));
  }
  return el;
}

function renderText(text: Text): HTMLElement {
  const el = document.createElement("span");
  el.dataset.poppyText = "";
  if (text.id) el.id = text.id;

  const classes = ["poppy-text"];
  if (text.color) classes.push(`poppy-text--color-${text.color}`);
  if (text.size) classes.push(`poppy-text--size-${text.size}`);
  if (text.weight) classes.push(`poppy-text--weight-${text.weight}`);
  el.className = classes.join(" ");

  el.textContent = text.value;
  return el;
}

function renderImage(image: Image, host: PoppyHost): HTMLElement {
  const allow = host.isUrlAllowed ?? isUrlAllowedDefault;
  const el = document.createElement("img");
  el.dataset.poppyImage = "";
  if (image.id) el.id = image.id;

  const classes = ["poppy-image"];
  if (image.fit) classes.push(`poppy-image--fit-${image.fit}`);
  el.className = classes.join(" ");

  el.alt = image.alt;
  if (allow(image.url, "image")) {
    el.src = image.url;
  } else {
    reportError(host, new Error(`Poppy: rejected image URL "${image.url}"`));
  }
  if (image.width !== undefined) el.width = image.width;
  if (image.height !== undefined) el.height = image.height;
  return el;
}

function renderButton(button: Button, host: PoppyHost, signal: AbortSignal): HTMLElement {
  const el = document.createElement("button");
  el.type = "button";
  el.dataset.poppyButton = "";
  if (button.id) el.id = button.id;
  el.className = "poppy-button";
  el.textContent = button.label;

  el.addEventListener(
    "click",
    () => {
      host.onAction(button.action);
    },
    { signal },
  );
  return el;
}
