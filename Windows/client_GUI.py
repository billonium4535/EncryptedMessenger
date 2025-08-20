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
    def __init__(self, root, username, room, password, on_close=None):
        self.on_close = None
        self.root = root
        self.root.title(f"{GUI_TITLE} - {room}")
        self.root.geometry("600x500")
        self.root.minsize(350, 400)
        self.root.protocol("WM_DELETE_WINDOW", self.close_window)

        self.username = username
        self.room = room
        self.on_close = on_close

        # Top bar
        top_frame = tk.Frame(root)
        top_frame.pack(fill="x", padx=5, pady=5)

        # Exit button
        self.exit_button = tk.Button(
            top_frame,
            text="X",
            command=self.exit_to_login,
            fg="red",
            relief="flat",
            borderwidth=0,
            cursor="hand2"
        )
        self.exit_button.grid(row=0, column=0, sticky="w")
        ToolTip(self.exit_button, "Back to login")

        # Room
        tk.Label(top_frame, text=f"Room: {self.room}", fg="black", font=("Arial", 12)).grid(row=0, column=1,
                                                                                            sticky="nsew")

        # Connecting status
        self.status_label = tk.Label(top_frame, text="(disconnected)", fg="red")
        self.status_label.grid(row=0, column=2, sticky="e")

        top_frame.grid_columnconfigure(1, weight=1)

        # Message display area
        self.chat_display = scrolledtext.ScrolledText(root, wrap=tk.WORD, state="disabled", height=20, width=60)
        self.chat_display.pack(padx=10, pady=10, fill="both", expand=True)

        # Input box
        self.entry = tk.Entry(root, width=60)
        self.entry.pack(padx=10, pady=(0, 10), fill="x")
        self.entry.bind("<Return>", self.send_message)

        # Setup Client
        self.client = Client(SERVER_IP, int(SERVER_PORT), message_callback=self.display_message,
                             status_callback=self.update_status)

        # Connect client
        self.client.connect(username, room, password)

    def update_status(self, status):
        if not self.root.winfo_exists():
            return

        def _update():
            if status == "connected":
                self.status_label.config(text="(connected)", fg="green")
            elif status == "reconnecting":
                self.status_label.config(text="(reconnecting...)", fg="orange")
            else:
                self.status_label.config(text="(disconnected)", fg="red")

        self.root.after(0, _update)

    def display_message(self, message):
        if self.root.winfo_exists():
            self.root.after(0, self._update_chat_display, message)

    def _update_chat_display(self, message):
        self.chat_display.config(state="normal")
        self.chat_display.insert(tk.END, message + "\n")
        self.chat_display.see(tk.END)
        self.chat_display.config(state="disabled")

    def send_message(self, event=None):
        message = self.entry.get().strip()
        if message:
            self.client.send_message(message)
            self.entry.delete(0, tk.END)

    def exit_to_login(self):
        try:
            self.client.disconnect()
        except Exception:
            pass
        if self.on_close:
            self.root.destroy()
            self.on_close()

    def close_window(self):
        try:
            self.client.disconnect()
        except Exception:
            pass
        self.root.destroy()
        sys.exit(0)
