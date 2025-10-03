import tkinter as tk
from tkinter import messagebox, ttk
from client_GUI import EncryptedMessengerGUI
from Config.config_reader import config_parser
from helper_functions import input_validate, length_validate

# Read the GUI title from the configuration file
GUI_TITLE = config_parser("./Config/client_config.ini", "GUI", "TITLE")

# Define a global background color constant for consistent styling
BACKGROUND_COLOR = "#212121"


class LoginGUI:
    """
    Login GUI class for handling user login before entering the chat room.

    Attributes:
        root (tk.Tk): The root Tkinter window.
        username_entry (tk.Entry): Entry widget for the username.
        room_entry (tk.Entry): Entry widget for the room.
        password_entry (tk.Entry): Entry widget for the password.
        connect_button (ttk.Button): Button to attempt connecting to the chat.
    """
    def __init__(self, root):
        """
        Initialise the Login GUI window and set up widgets.

        Args:
            root (tk.Tk): The main Tkinter root window.
        """
        self.root = root
        self.root.title(f"Login - {GUI_TITLE}")
        self.root.resizable(False, False)
        self.root.geometry("300x350")
        self.root.configure(bg=BACKGROUND_COLOR)
        # Clicking outside entries unfocuses them
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
                        background=BACKGROUND_COLOR, )

        style.map("Rounded.TButton",
                  background=[("active", BACKGROUND_COLOR,)])

        # Connect button
        self.connect_button = ttk.Button(root, text="Connect", style="Rounded.TButton", command=self.connect)
        self.connect_button.pack(side="bottom", pady=20, ipadx=20)

    def unfocus_all(self, event):
        """
        Unfocus any Entry widgets when clicking elsewhere in the window.

        Args:
            event: The Tkinter event triggered by a mouse click.
        """
        widget = event.widget
        if not isinstance(widget, tk.Entry):
            self.root.focus()

    def connect(self):
        """
        Attempt to connect to the chat system after validating input fields.
        Shows warning messages if validation fails.
        """
        username = self.username_entry.get().strip()
        room = self.room_entry.get().strip()
        password = self.password_entry.get().strip()

        # Validation
        fields = [
            {
                "name": "username",
                "value": username,
                "validators": [
                    (lambda v, _: input_validate(v, "username"), "Username must contain only letters and numbers"),
                    (lambda v, _: length_validate(v, 20), "Username must be less than 20 characters")
                ]
            },
            {
                "name": "room",
                "value": room,
                "validators": [
                    (lambda v, _: input_validate(v, "room"),
                     "Room must contain only letters, numbers and special characters"),
                    (lambda v, _: length_validate(v, 20), "Room must be less than 20 characters")
                ]
            },
            {
                "name": "password",
                "value": password,
                "validators": [
                    (lambda v, _: input_validate(v, "password"),
                     "Password must contain only letters, numbers and special characters"),
                    (lambda v, _: length_validate(v, 20), "Password must be less than 20 characters")
                ]
            }
        ]

        # check for empty fields
        if not all(f["value"] for f in fields):
            messagebox.showwarning("Missing Info", "Please fill in all fields")
            return

        # run validations
        for field in fields:
            for validator, error_msg in field["validators"]:
                if not validator(field["value"], field["name"]):
                    messagebox.showwarning(f"Invalid {field['name'].capitalize()}", error_msg)
                    return

        self.root.withdraw()
        chat_window = tk.Toplevel()
        EncryptedMessengerGUI(chat_window, username, room, password, on_close=self.root.deiconify)


if __name__ == "__main__":
    root = tk.Tk()
    login_gui = LoginGUI(root)
    root.mainloop()
