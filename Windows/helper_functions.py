import tkinter as tk
import re


class ToolTip:
    def __init__(self, widget, text):
        self.widget = widget
        self.text = text
        self.tooltip_window = None
        widget.bind("<Enter>", self.show_tooltip)
        widget.bind("<Leave>", self.hide_tooltip)

    def show_tooltip(self, event=None):
        if self.tooltip_window or not self.text:
            return
        x = self.widget.winfo_rootx() + 20
        y = self.widget.winfo_rooty() + self.widget.winfo_height() + 5
        self.tooltip_window = tw = tk.Toplevel(self.widget)
        tw.wm_overrideredirect(True)  # remove window borders
        tw.wm_geometry(f"+{x}+{y}")
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
        tw = self.tooltip_window
        self.tooltip_window = None
        if tw:
            tw.destroy()


def input_validate(text: str, validation_type: str) -> bool:
    """
    Validates an input against regex

    Args:
        text (str): The text.
        validation_type (str): The validation type ('username').

    Returns:
        bool: If it passes the validation.
    """
    value = False
    if validation_type == "username":
        if re.match(r'^[A-Za-z0-9]*$', text):
            value = True
        else:
            value = False

    return value


def length_validate(text: str, length: int) -> bool:
    """
    Validates an input against regex

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
