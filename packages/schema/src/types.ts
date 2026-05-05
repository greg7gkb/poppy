// Hand-written TypeScript types for Poppy v0.1 documents.
//
// These mirror the JSON Schema files in ../schemas/. The conformance corpus
// (Phase 1+) catches any drift between these types and the schema.
//
// Convention:
//   - Component `type` discriminator values are PascalCase ("Stack", "Text", ...).
//   - Action `type` discriminator values are lowercase ("navigate", ...).
//   - All other field names are camelCase.

/** Wire-format version this package targets. See ADR-0006 for versioning rules. */
export const SCHEMA_VERSION = "0.1";

// --- Tokens ---------------------------------------------------------------

export type Spacing = "none" | "xs" | "sm" | "md" | "lg" | "xl";
export type Size = "xs" | "sm" | "md" | "lg" | "xl";
export type Color = "default" | "primary" | "secondary" | "danger" | "success";
export type Weight = "regular" | "medium" | "bold";
export type Alignment = "start" | "center" | "end" | "stretch";
export type Fit = "contain" | "cover" | "fill";
export type Axis = "horizontal" | "vertical";

// --- Components -----------------------------------------------------------

export interface Stack {
  type: "Stack";
  id?: string;
  axis: Axis;
  children: Component[];
  spacing?: Spacing;
  padding?: Spacing;
  alignment?: Alignment;
}

export interface Text {
  type: "Text";
  id?: string;
  value: string;
  color?: Color;
  size?: Size;
  weight?: Weight;
}

export interface Image {
  type: "Image";
  id?: string;
  url: string;
  alt: string;
  width?: number;
  height?: number;
  fit?: Fit;
}

export interface Button {
  type: "Button";
  id?: string;
  label: string;
  action: Action;
}

export type Component = Stack | Text | Image | Button;

// --- Actions --------------------------------------------------------------

export interface NavigateAction {
  type: "navigate";
  uri: string;
}

export type Action = NavigateAction;

// --- Document -------------------------------------------------------------

export interface PoppyDocument {
  /** Optional canonical schema URL. Editors use this for autocomplete; renderers ignore it. */
  $schema?: string;
  /** Wire-format version, MAJOR.MINOR. Required. */
  version: string;
  /** The single root component. */
  root: Component;
}
