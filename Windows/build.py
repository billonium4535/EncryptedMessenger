import subprocess
import sys

# Variables
MAIN_SCRIPT = "login_GUI.py"
EXE_NAME = "EncryptedMessenger"
DATA_FOLDERS = ["./Config/"]

# Build PyInstaller command
command = [
    sys.executable, "-m", "PyInstaller",    # Use the current Python interpreter
    "--onefile",                            # Package into a single executable
    "--windowed",                           # Suppress console window (use --console for debugging)
    "--name",
    EXE_NAME
]

# Add each data folder to the build command
for folder in DATA_FOLDERS:
    command.append(f"--add-data={folder};{folder}")

# Append the main script to build
command.append(MAIN_SCRIPT)

print("Running build command:")
print(" ".join(command))

# Run the build process with error checking enabled
subprocess.run(command, check=True)

print("\nBuild finished and moved to 'dist' folder.")
