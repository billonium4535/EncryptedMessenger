import tkinter as tk
from tkinter import scrolledtext
import sys

from client import Client
from Config.config_reader import config_parser
from helper_functions import ToolTip

SERVER_IP = config_parser("../Config/client_config.ini", "DEFAULT", "IP_ADDRESS")
SERVER_PORT = config_parser("../Config/client_config.ini", "DEFAULT", "PORT")
GUI_TITLE = config_parser("../Config/client_config.ini", "GUI", "TITLE")


class EncryptedMessengerGUI:
    def __init__(self, root):
        self.root = root
        self.root.title(GUI_TITLE)

        # Message display area
        self.chat_display = scrolledtext.ScrolledText(root, wrap=tk.WORD, state="disabled", height=20, width=60)
        self.chat_display.pack(padx=10, pady=10, fill="both", expand=True)

        # Input box
        self.entry = tk.Entry(root, width=60)
        self.entry.pack(padx=10, pady=(0, 10), fill="x")
        self.entry.bind("<Return>", self.send_message)

        # Setup Client
        self.client = Client(SERVER_IP, int(SERVER_PORT), message_callback=self.display_message)

        # Ask for creds
        name = simpledialog.askstring("Login", "Enter your name:", parent=root)
        room = simpledialog.askstring("Room", "Enter room:", parent=root)
        password = simpledialog.askstring("Password", "Enter password:", parent=root, show="*")

        if not name or not room or not password:
            root.destroy()
            return

        self.client.connect(name, room, password)

    def display_message(self, message):
        self.chat_display.config(state="normal")
        self.chat_display.insert(tk.END, message + "\n")
        # Auto scroll
        self.chat_display.see(tk.END)
        self.chat_display.config(state="disabled")

    def send_message(self, event=None):
        message = self.entry.get().strip()
        if message:
            self.client.send_message(message)
            # self.display_message(f"{self.client.name}: {message}")
            self.entry.delete(0, tk.END)


if __name__ == "__main__":
    root = tk.Tk()
    gui = EncryptedMessengerGUI(root)
    root.mainloop()
