import React, { useState } from "react";
//import { useParams } from "react-router";
import { useParams } from "react-router-dom";

function RoomPage() {
  const [desiredTemp, setDesiredTemp] = useState(20);
  const [currentTemp, setCurrentTemp] = useState(22);
  let { roomNum } = useParams();

  const fetchData = async () => {
    try {
      const response = await fetch(`http://localhost:8080/currentTemp?roomNum=${roomNum}`);
      const jsonData = await response.json();
      setCurrentTemp(jsonData);

      console.log("Data received:", jsonData); // Log data to the console
    } catch (error) {
      console.error("Error fetching data:", error);
    }
  };

  // Function to increment desired temperature
  const incrementDesiredTemp = () => {
    // Update state with incremented value
    setDesiredTemp(desiredTemp + 1);
  };

  // Function to decrement desired temperature
  const decrementDesiredTemp = () => {
    // Update state with decremented value
    setDesiredTemp(desiredTemp - 1);
  };

  // Data to send with the request
  const data = {
    type: 1,
    room: roomNum,
    temperature: desiredTemp,
  };

  const url = "http://localhost:8080/endpoint";
  // Options for the fetch request
  const options = {
    method: "POST", // or 'GET', 'PUT', 'DELETE', etc.
    body: JSON.stringify(data), // Convert data to JSON string
  };

  const setTemperature = () => {
    console.log("Setting temperature");

    fetch(url, options)
      .then((data) => {
        console.log("Response:", data);
      })
      .catch((error) => {
        console.error("There was a problem with the fetch operation:", error);
      });
  };

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