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

# How to setup (Linux/Windows)
- First you need MySQL installed. 
- Next you need to set up a username and password for MySQL. Then you need to create a database. Remember all of these! (There are many tutorials out there explaining how to do these steps)
- Next go to src/ServerPackage/MySqlConn.java
- In this class, you will see three things you need to change, 1. the database name, the username and the password of your MySQL local account
- Next we do need to set up port forwarding (TCP connection). (Again there are very good tutorials out there for this step)
- The default port I have this server listen on is *9663*. You can change this in src/ServerPackage/Server.Java where you should see a PORT variable that you can change.
- * To run the server in Linux you must be in the src directory, we must compile everything using this line -> javac -cp "java-json.jar:mysql-connector-java-8.0.21.jar ChatImplementations/*.java Interfaces/*.java SerializationClasses/*.java ServerPackage/*.java" (Without quotes) Then we can run the server by using this line -> "java -cp .:java-json.jar:mysql-connector-java-8.0.21.jar ServerPackage.Server"
- * Also you might have to change the .classpath file to link the two external libraries (java-json & mysql-connector)
- Now the server should run and display these two lines, "Database Connection has been established!", and "Server is listening on port (Your port goes here)". 
- Next we have to set up the client side if you have not already. This repo can be found here: https://github.com/HusamSaleem/Potato_Messenger_Client
