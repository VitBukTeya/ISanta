ISanta - Secrete Santa Telegram Bot for Free

/help - shows list of commands
/create - creates the event [person assign as host]
/start - you need to write that in the private chat with bot, thats how Telegram API works
/join - participant registered for the event
/leave - participant leave the event
/list - list of registered participants (also shows who already wrote /start in private chat with bot)
/start_event - if everyone is ready (registered+wrote /start in private chat) host can initiate start of the event
Bot will randomly (satollo's algorithm) assign participants to givers and recievers (no self-givers)

TODO: 
EventServce instead of huge Main.kt
CommandFactory
Some text editing
