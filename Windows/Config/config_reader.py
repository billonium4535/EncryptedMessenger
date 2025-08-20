import configparser
import os
import sys


def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and PyInstaller """
    if hasattr(sys, '_MEIPASS'):
        # PyInstaller puts files here
        return os.path.join(sys._MEIPASS, relative_path)
    return os.path.join(os.path.abspath("."), relative_path)


def config_parser(config_file, config_section, config_key):
    config = configparser.ConfigParser()
    config.read(resource_path(config_file))
    return config[config_section][config_key]
