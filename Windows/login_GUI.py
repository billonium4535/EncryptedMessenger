import tkinter as tk
from tkinter import messagebox, ttk
from client_GUI import EncryptedMessengerGUI
from Config.config_reader import config_parser


GUI_TITLE = config_parser("./Config/client_config.ini", "GUI", "TITLE")
BACKGROUND_COLOR = "#212121"


class LoginGUI:
    def __init__(self, root):
        self.root = root
        self.root.title(f"Login - {GUI_TITLE}")
        self.root.resizable(False, False)
        self.root.geometry("300x350")
        self.root.configure(bg=BACKGROUND_COLOR)
        self.root.bind("<Button-1>", self.unfocus_all)

        # Title
        tk.Label(root, text=GUI_TITLE, font=("Arial", 12, "bold"), bg=BACKGROUND_COLOR, fg="white").pack(pady=(5, 10))

        # Username
        tk.Label(root, text="Username:", bg=BACKGROUND_COLOR, fg="white").pack(pady=(40, 0))
        self.username_entry = tk.Entry(root, width=30, bg="#F5FEFD", fg="black", borderwidth=0)
        self.username_entry.pack(pady=5)

        # Room
        tk.Label(root, text="Room:", bg=BACKGROUND_COLOR, fg="white").pack(pady=0)
        self.room_entry = tk.Entry(root, width=30, bg="#F5FEFD", fg="black", borderwidth=0)
        self.room_entry.pack(pady=5)

        # Password
        tk.Label(root, text="Password:", bg=BACKGROUND_COLOR, fg="white").pack(pady=0)
        self.password_entry = tk.Entry(root, width=30, show="*", bg="#F5FEFD", fg="black", borderwidth=0)
        self.password_entry.pack(pady=5)

        # Button
        style = ttk.Style()
        style.configure("Rounded.TButton",
                        font=("Arial", 12),
                        padding=10,
                        relief="flat",
                        borderwidth=0,
                        foreground="#3d3d3d",
                        background=BACKGROUND_COLOR,)

        style.map("Rounded.TButton",
                  background=[("active", BACKGROUND_COLOR, )])

        self.connect_button = ttk.Button(root, text="Connect", style="Rounded.TButton", command=self.connect)
        self.connect_button.pack(side="bottom", pady=20, ipadx=20)

    def unfocus_all(self, event):
        widget = event.widget
        if not isinstance(widget, tk.Entry):
            self.root.focus()

    def connect(self):
        username = self.username_entry.get().strip()
        room = self.room_entry.get().strip()
        password = self.password_entry.get().strip()

        if not username or not room or not password:
            messagebox.showwarning("Missing Info", "Please fill in all fields")
            return

        # Hide login window
        self.root.withdraw()
        chat_window = tk.Toplevel()
        EncryptedMessengerGUI(chat_window, username, room, password, on_close=self.root.deiconify)


if __name__ == "__main__":
    root = tk.Tk()
    login_gui = LoginGUI(root)
    root.mainloop()

