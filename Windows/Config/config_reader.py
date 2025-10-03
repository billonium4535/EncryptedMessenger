import configparser
import os
import sys


def resource_path(relative_path):
    """
    Get absolute path to resource, works for dev and PyInstaller

    Args:
        relative_path (str): The relative path to the resource file.

    Returns:
        str: The absolute path to the resource file.
    """
    if hasattr(sys, '_MEIPASS'):
        # PyInstaller puts files here
        return os.path.join(sys._MEIPASS, relative_path)
    return os.path.join(os.path.abspath("."), relative_path)


def config_parser(config_file, config_section, config_key):
    """
    Parse a configuration file and return a specific value.

    Args:
        config_file (str): Path to the configuration file.
        config_section (str): Section in the INI file.
        config_key (str): Key within the section to retrieve.

    Returns:
        str: The configuration value corresponding to the given section and key.

    Raises:
        KeyError: If the section or key does not exist in the config file.
    """
    config = configparser.ConfigParser()
    # Load the config file
    config.read(resource_path(config_file))
    # Return the requested value
    return config[config_section][config_key]
