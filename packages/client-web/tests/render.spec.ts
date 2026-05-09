// Behavioral tests for render(): host callbacks, validation, sanitization,
// destroy(), update(). Snapshot equivalence lives in corpus.spec.ts.

import type { NavigateAction } from "@poppy/schema";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { isUrlAllowedDefault, render } from "../src/index.js";

let container: HTMLElement;

beforeEach(() => {
  document.body.replaceChildren();
  container = document.createElement("div");
  document.body.appendChild(container);
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("Button → onAction", () => {
  it("forwards the action verbatim on click", () => {
    const onAction = vi.fn();
    const action: NavigateAction = { type: "navigate", uri: "poppy://home" };
    render({ version: "0.1", root: { type: "Button", label: "Go", action } }, container, {
      onAction,
    });
    container.querySelector("button")!.click();
    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onAction).toHaveBeenCalledWith(action);
  });
});

describe("Image URL sanitization", () => {
  it("rejects javascript: URLs by default and reports an error", () => {
    const onError = vi.fn();
    render(
      {
        version: "0.1",
        root: { type: "Image", url: "javascript:alert(1)", alt: "x" },
      },
      container,
      { onAction: () => {}, onError },
    );
    const img = container.querySelector("img")!;
    expect(img.getAttribute("src")).toBe(null);
    expect(onError).toHaveBeenCalledTimes(1);
  });

  it("rejects data:text/html URLs by default", () => {
    expect(isUrlAllowedDefault("data:text/html,<script>", "image")).toBe(false);
  });

  it("allows http/https/relative/data:image URLs by default", () => {
    expect(isUrlAllowedDefault("https://example.com/x.png", "image")).toBe(true);
    expect(isUrlAllowedDefault("http://example.com/x.png", "image")).toBe(true);
    expect(isUrlAllowedDefault("/relative.png", "image")).toBe(true);
    expect(isUrlAllowedDefault("data:image/png;base64,AAAA", "image")).toBe(true);
  });

  it("honors the host.isUrlAllowed override", () => {
    render(
      {
        version: "0.1",
        root: { type: "Image", url: "weird://thing", alt: "x" },
      },
      container,
      { onAction: () => {}, isUrlAllowed: () => true },
    );
    expect(container.querySelector("img")!.getAttribute("src")).toBe("weird://thing");
  });
});

describe("Validation", () => {
  it("does not render when validation fails (default)", () => {
    const onError = vi.fn();
    render({ version: "0.1", root: { type: "Wat" } }, container, {
      onAction: () => {},
      onError,
    });
    expect(container.children.length).toBe(0);
    expect(onError).toHaveBeenCalledTimes(1);
  });

  it("rejects an unsupported version (ADR-0006)", () => {
    const onError = vi.fn();
    render({ version: "999.0", root: { type: "Text", value: "x" } }, container, {
      onAction: () => {},
      onError,
    });
    expect(container.children.length).toBe(0);
    expect(onError).toHaveBeenCalledTimes(1);
  });

  it("skips validation when host.validate === false", () => {
    render({ version: "0.1", root: { type: "Text", value: "trusted" } }, container, {
      onAction: () => {},
      validate: false,
    });
    expect(container.querySelector("span")!.textContent).toBe("trusted");
  });
});

describe("update() / destroy()", () => {
  it("update() replaces the tree", () => {
    const r = render({ version: "0.1", root: { type: "Text", value: "first" } }, container, {
      onAction: () => {},
    });
    expect(container.querySelector("span")!.textContent).toBe("first");
    r.update({ version: "0.1", root: { type: "Text", value: "second" } });
    expect(container.querySelector("span")!.textContent).toBe("second");
  });

  it("destroy() clears the container and detaches button listeners", () => {
    const onAction = vi.fn();
    const r = render(
      {
        version: "0.1",
        root: { type: "Button", label: "Go", action: { type: "navigate", uri: "x" } },
      },
      container,
      { onAction },
    );
    const button = container.querySelector("button")!;
    r.destroy();
    expect(container.children.length).toBe(0);
    button.click();
    expect(onAction).not.toHaveBeenCalled();
  });

  it("destroy() is idempotent", () => {
    const r = render({ version: "0.1", root: { type: "Text", value: "x" } }, container, {
      onAction: () => {},
    });
    r.destroy();
    expect(() => r.destroy()).not.toThrow();
  });
});

describe("Text safety", () => {
  it("does not interpret HTML in Text.value", () => {
    render({ version: "0.1", root: { type: "Text", value: "<script>x</script>" } }, container, {
      onAction: () => {},
    });
    const span = container.querySelector("span")!;
    expect(span.textContent).toBe("<script>x</script>");
    expect(span.querySelector("script")).toBe(null);
  });
});
