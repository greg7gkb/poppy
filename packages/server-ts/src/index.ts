// Public entry for `@poppy/server-ts`.

export { isValid, validate } from "./validate.js";
export type { ValidationError, ValidationResult } from "./validate.js";

export { SCHEMA_VERSION } from "@poppy/schema";

export type {
  Action,
  Alignment,
  Axis,
  Button,
  Color,
  Component,
  Fit,
  Image,
  NavigateAction,
  PoppyDocument,
  Size,
  Spacing,
  Stack,
  Text,
  Weight,
} from "@poppy/schema";
