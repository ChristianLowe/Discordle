# Discordle
This Discord bot is a fun game that lets your community play "Wordle"-like puzzles cooperatively.

## Invite to your server
You can invite Discordle to your server with this link:
https://discord.com/api/oauth2/authorize?client_id=944401438233202728&permissions=2048&scope=bot%20applications.commands

## Prerequisites to Install

* [Apache Maven 3.9+](https://maven.apache.org/download.cgi)
* [Docker](https://docs.docker.com/install/)

## Installing

Before getting started, you will need to [create your bot through Discord](https://discordapp.com/developers/applications/me).
After getting the credentials for your new bot, export a new environmental variable named `DISCORDLE_TOKEN` which
holds your bots API token. When you run your first command, your user will be added to the users table; you can
set yourself as an admin in this table which will allow your Discord ID to run sensitive commands. 
To find your Discord ID, right-click your self on Discord and click "Copy ID".

After cloning this repository, run this command in the project's root folder: `chmod +x run.sh && ./run.sh`

## Contributing

Thank you for your interest. If you'd like to contribute, please fork the repository and use a feature
branch. Pull requests are warmly welcome. Alternatively, let me know of any issues you face by filing a
report in the project's [issue tracker](https://github.com/ChristianLowe/Discordle/issues).

## Licensing

The code in this project is licensed under [the MIT license](https://tldrlegal.com/license/mit-license).

The dictionary and wordlist are from the official [Wordle game](https://www.nytimes.com/games/wordle/index.html),
developed by Josh Wardle.
