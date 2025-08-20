import subprocess
import sys

MAIN_SCRIPT = "login_GUI.py"
EXE_NAME = "EncryptedMessenger"
DATA_FOLDERS = ["./Config/"]

command = [
    sys.executable, "-m", "PyInstaller",
    "--onefile",
    "--windowed",  # --console
    "--name",
    EXE_NAME
]

for folder in DATA_FOLDERS:
    command.append(f"--add-data={folder};{folder}")

command.append(MAIN_SCRIPT)

print("Running build command:")
print(" ".join(command))

subprocess.run(command, check=True)

print("\nBuild finished and moved to 'dist' folder.")
