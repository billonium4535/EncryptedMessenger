import tkinter as tk
import re


class ToolTip:
    """
    A simple tooltip implementation for Tkinter widgets.

    Displays a small popup window with text when the mouse hovers over the widget, and hides it when the mouse leaves.

    Attributes:
        widget (tk.Widget): The Tkinter widget to attach the tooltip to.
        text (str): The text displayed in the tooltip.
        tooltip_window (tk.Toplevel): The popup window used for the tooltip.
    """
    def __init__(self, widget, text):
        """
        Initialize the tooltip and bind mouse events.

        Args:
            widget (tk.Widget): The widget to attach the tooltip to.
            text (str): The text to display in the tooltip.
        """
        self.widget = widget
        self.text = text
        self.tooltip_window = None

        # Bind mouse enter/leave events to show/hide tooltip
        widget.bind("<Enter>", self.show_tooltip)
        widget.bind("<Leave>", self.hide_tooltip)

    def show_tooltip(self, event=None):
        """
        Display the tooltip near the widget when the mouse hovers over it.

        Args:
            event: Optional Tkinter event object (not used directly).
        """
        if self.tooltip_window or not self.text:
            # Prevent duplicate tooltips or empty text
            return

        # Position tooltip slightly offset from the widget
        x = self.widget.winfo_rootx() + 20
        y = self.widget.winfo_rooty() + self.widget.winfo_height() + 5

        # Create a borderless Toplevel window for the tooltip
        self.tooltip_window = tw = tk.Toplevel(self.widget)
        # remove window borders
        tw.wm_overrideredirect(True)
        tw.wm_geometry(f"+{x}+{y}")

        # Add a label inside the tooltip window
        label = tk.Label(
            tw,
            text=self.text,
            background="white",
            relief="solid",
            borderwidth=1,
            padx=4,
            pady=2
        )
        label.pack()

    def hide_tooltip(self, event=None):
        """
        Destroy the tooltip when the mouse leaves the widget.

        Args:
            event: Optional Tkinter event object (not used directly).
        """
        tw = self.tooltip_window
        self.tooltip_window = None
        if tw:
            tw.destroy()


def input_validate(text: str, validation_type: str) -> bool:
    """
    Validates an input against regex

    Args:
        text (str): The text.
        validation_type (str): The validation type ('username', 'room', or 'password').

    Returns:
        bool: If it passes the validation.
    """
    value = False
    if validation_type == "username":
        if re.match(r'^[A-Za-z0-9]*$', text):
            value = True
        else:
            value = False
    if validation_type == "room":
        if re.match(r'^[A-Za-z0-9]*$', text):
            value = True
        else:
            value = False
    if validation_type == "password":
        if re.match(r'^[A-Za-z0-9]*$', text):
            value = True
        else:
            value = False

    return value


def length_validate(text: str, length: int) -> bool:
    """
    Validates an input against the length

    Args:
        text (str): The text.
        length (int): The length of the text.

    Returns:
        bool: If it passes the validation.
    """
    if len(text) <= length:
        value = True
    else:
        value = False
    return value
