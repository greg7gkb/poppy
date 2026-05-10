// The Poppy document this example renders.
//
// In a real app, this JSON would come from a network response — the server
// holds the document, the client validates and renders. For the example we
// inline a copy of the kitchen-sink corpus case so the example is
// self-contained and works offline.
//
// Edit `kitchenSinkJSON` to experiment: change tokens, swap component types,
// try invalid inputs to see the validator's error messages.

import Foundation

enum SampleDocument {
    static let kitchenSinkJSON: Data = """
    {
      "version": "0.1",
      "root": {
        "type": "Stack",
        "id": "screen-root",
        "axis": "vertical",
        "spacing": "md",
        "padding": "lg",
        "alignment": "stretch",
        "children": [
          {
            "type": "Stack",
            "axis": "horizontal",
            "spacing": "sm",
            "alignment": "center",
            "children": [
              {
                "type": "Image",
                "url": "https://avatars.githubusercontent.com/u/1390?v=4",
                "alt": "User avatar",
                "width": 48,
                "height": 48,
                "fit": "cover"
              },
              {
                "type": "Stack",
                "axis": "vertical",
                "spacing": "xs",
                "children": [
                  { "type": "Text", "value": "Greg", "size": "lg", "weight": "bold" },
                  { "type": "Text", "value": "Signed in", "size": "sm", "color": "secondary" }
                ]
              }
            ]
          },
          { "type": "Text", "value": "You have 3 unread notifications.", "color": "primary" },
          {
            "type": "Stack",
            "axis": "horizontal",
            "spacing": "sm",
            "alignment": "end",
            "children": [
              {
                "type": "Button",
                "label": "Dismiss",
                "action": { "type": "navigate", "uri": "poppy://notifications/dismiss-all" }
              },
              {
                "type": "Button",
                "label": "View all",
                "action": { "type": "navigate", "uri": "poppy://notifications" }
              }
            ]
          }
        ]
      }
    }
    """.data(using: .utf8)!
}
