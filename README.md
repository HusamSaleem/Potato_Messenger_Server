# Potato Messenger Server

# Why I created it
- A nice simplified replica of a chat system like Facebook's messenger. I created this to learn more about networking, MySQL, AWS and Android Studio so it isn't 
really meant for anything outside of personal use or just for fun. Almost 5k lines of code combined with the client side :o

# Description 
- This is the server side of the app. It is made using the Java programming language. Main Libraries used: Java.Net, an external java-json library, mysql-connector library, and some others. This can be hosted on really any computer with JVM (Java virtual machine). I used AWS to host it when I want to run this app. 

# Server Side Features
- Removes clients from the active client list for inactivity
- Threadpool is used to help manage resources instead of creating way too many threads
- Uses normal sockets
- Can add features easily with how its organized
- Documented code
- Minimimal delay to send data back to the client

# Known Bugs
- When you click the same chat (Either a global chat room or private) it will duplicate the messages
- The knowing when your friends are online is sometimes glitchy and doesn't work

# How to setup
