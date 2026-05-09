// Smoke example: wire @poppy/client-web into a real browser.
//
// Edit `document` below to see the renderer respond. The shape is fully
// described by the JSON Schema at packages/schema/schemas/poppy.schema.json
// and the TypeScript types at packages/schema/src/types.ts.

import { render } from "../../packages/client-web/dist/index.js";

const document_ = {
  version: "0.1",
  root: {
    type: "Stack",
    axis: "vertical",
    spacing: "md",
    padding: "md",
    children: [
      { type: "Text", value: "Welcome to Poppy", size: "xl", weight: "bold" },
      {
        type: "Text",
        value: "This entire panel is rendered from a JSON document. No JSX, no DSL — just data.",
        color: "secondary",
      },
      {
        type: "Stack",
        axis: "horizontal",
        spacing: "sm",
        alignment: "center",
        children: [
          {
            type: "Image",
            url: "https://avatars.githubusercontent.com/u/1390?v=4",
            alt: "GitHub avatar placeholder",
            width: 48,
            height: 48,
            fit: "cover",
          },
          {
            type: "Stack",
            axis: "vertical",
            spacing: "xs",
            children: [
              { type: "Text", value: "Greg", weight: "bold" },
              { type: "Text", value: "Signed in", size: "sm", color: "secondary" },
            ],
          },
        ],
      },
      {
        type: "Stack",
        axis: "horizontal",
        spacing: "sm",
        children: [
          {
            type: "Button",
            label: "Open profile",
            action: { type: "navigate", uri: "poppy://profile" },
          },
          {
            type: "Button",
            label: "Sign out",
            action: { type: "navigate", uri: "poppy://auth/sign-out" },
          },
        ],
      },
    ],
  },
};

const log = window.document.getElementById("log");

const handle = render(document_, window.document.getElementById("app"), {
  onAction(action) {
    if (log.querySelector(".empty")) log.textContent = "";
    const ts = new Date().toLocaleTimeString();
    log.textContent += `[${ts}] ${JSON.stringify(action)}\n`;
  },
  onError(err) {
    console.error("Poppy error:", err);
  },
});

// Expose for poking around in DevTools.
window.poppy = { handle, document: document_ };
