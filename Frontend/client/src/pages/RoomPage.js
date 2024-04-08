import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";

/**
 * Functional component representing the room page
 * Displays information about the room including current temperature and allows users to set desired temperature
 */
function RoomPage() {
   // Initialize state variables for desired and current temperature
  const [desiredTemp, setDesiredTemp] = useState(20);
  const [currentTemp, setCurrentTemp] = useState(null);

  // Initialize room number from URL parameters
  let { roomNum } = useParams();

  // Function to fetch current temperature data
  const fetchData = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/currentTemp?roomNum=${roomNum}`
      );
      const jsonData = await response.json();
      setCurrentTemp(jsonData);

      // Log data to the console
      console.log("Data received:", jsonData);
    } catch (error) {
      console.error("Error fetching data:", error);
      try {
        const response = await fetch(
            `http://localhost:8081/currentTemp?roomNum=${roomNum}`
        );
        const jsonData = await response.json();
        setCurrentTemp(jsonData);

        // Log data to the console
        console.log("Data received:", jsonData);
      } catch (error) {
        console.error("Error fetching data:", error);
      }
    }
  };

  // Run fetchData function when the component mounts
  useEffect(() => {
    fetchData();
  }, []); // Empty dependency array to ensure the effect runs only once on component mount

  // Function to increment desired temperature
  const incrementDesiredTemp = () => {
    setDesiredTemp(desiredTemp + 1);
  };

  // Function to decrement desired temperature
  const decrementDesiredTemp = () => {
    setDesiredTemp(desiredTemp - 1);
  };

  // Data to send with the request to set temperature
  const data = {
    type: 1,
    room: roomNum,
    temperature: desiredTemp,
  };

  // URLs for setting temperature with primary and backup endpoints
  const url = "http://localhost:8080/endpoint";
  const backupUrl = "http://localhost:8081/endpoint";

  // Options for the fetch request
  const options = {
    method: "POST", // or 'GET', 'PUT', 'DELETE', etc.
    body: JSON.stringify(data), // Convert data to JSON string
  };

  // Function to set temperature
  const setTemperature = () => {
    console.log("Setting temperature");

    fetch(url, options)
        .then((data) => {
          console.log("Response:", data);
        })
        .catch((error) => {
          fetch(backupUrl, options)
              .then((data) => {
                console.log("Response:", data);
              })
              .catch((error) => {
                console.error("There was a problem with the fetch operation:", error);
              });
        });
    alert("Temperature has been set");
  };

  // render room page form
  return (
    <div class="container my-5 d-flex flex-column align-items-center justify-content-center align-middle">
      <div class="card w-50 my-2">
        <div class="card-body">
          <h5 class="card-title text-center">Room Number</h5>
          <p class="card-text text-center">{roomNum}</p>
        </div>
      </div>

      <div class="card w-50 my-2">
        <div class="card-body">
          <h5 class="card-title text-center">Current Temperature</h5>
          <p class="card-text text-center">{currentTemp}</p>
          <div class="container d-flex align-items-center justify-content-center">
            <button
              type="increment"
              class="btn btn-primary mx-2"
              onClick={fetchData}
            >
              Refresh Current Temperature
            </button>
          </div>
        </div>
      </div>

      <div class="card w-50 my-2">
        <div class="card-body">
          <h5 class="card-title text-center">Set Temperature</h5>
          <p class="card-text text-center">{desiredTemp}</p>
          <div class="container d-flex align-items-center justify-content-center">
            <button
              type="increment"
              class="btn btn-primary mx-2"
              onClick={incrementDesiredTemp}
              id="setTempNotify"
            >
              Increase temperature
            </button>
            <button
              type="increment"
              class="btn btn-primary"
              onClick={decrementDesiredTemp}
            >
              Decrease temperature
            </button>
            <button
              type="increment"
              class="btn btn-primary mx-2"
              onClick={setTemperature}
            >
              Set temperature
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default RoomPage;
