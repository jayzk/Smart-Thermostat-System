# CPSC 559 Final Project (Distributed Systems)

## Running Frontend
1. Install node [here](https://nodejs.org/en/download)
> [!NOTE]
> With windows, there will be an option to install Chocolately, make sure to install that. Example of an installation can be seen here https://www.youtube.com/watch?v=EIzdQxMXcrc (just watch the first 4 mins)

2. Check if node is installed using the terminal
```
node --version
```
3. Open the frontend folder in your IDE (I would recommend using VS code)
4. Using the VS code terminal change the current directory to the client folder

![image](https://github.com/jayzk/Smart-Thermostat-System/assets/57610243/3358fb93-3061-44e6-80d5-acce3f6a9cc9)
![image](https://github.com/jayzk/Smart-Thermostat-System/assets/57610243/99bb9346-7917-4d47-9c87-99a36cd0dca4)

5. Run npm install in the terminal and wait for the packages to download
```
npm install
```
> [!NOTE]
> This only needs to be done once, you don't have to run this every time

6. Start the web app
```
npm run start
```

From here a page should open in your browser

## Login page
Here you will be prompted to input a room number and a password

![image](https://github.com/jayzk/Smart-Thermostat-System/assets/57610243/5403708c-b9c6-4e9b-af90-8f49612729ee)

- As of now room numbers 1-30 exist, any other room number inputted will result in a alert saying the room doesn't exist
- The password for each room is just "password" for simplicity sake
