import configparser


def config_parser(config_file, config_section, config_key):
    config = configparser.ConfigParser()
    config.read(config_file)
    return config[config_section][config_key]
