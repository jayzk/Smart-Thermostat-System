import React, { useState } from "react";

function RoomPage() {
  const [desiredTemp, setDesiredTemp] = useState(20);
  const [currentTemp, setCurrentTemp] = useState(22);
  const [roomNum, setRoomNum] = useState(1);

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
    room: 1,
    temperature: desiredTemp,
  };

  const url = "http://localhost:8080/";
  // Options for the fetch request
  const options = {
    method: "POST", // or 'GET', 'PUT', 'DELETE', etc.
    // headers: {
    //   "Content-Type": "application/json",
    //   // Add any other headers as needed
    // },
    body: JSON.stringify(data), // Convert data to JSON string
  };

  const setTemperature = () => {
    console.log("bttn clicked");

    fetch(url, options)
      .then((response) => {
        console.log("ERROR WITH NETWORK");
        if (!response.ok) {
          throw new Error("Network response was not ok");
        }
        return response.json(); // Parse response body as JSON
      })
      .then((data) => {
        console.log("Response:", data);
        // Do something with the response data
      })
      .catch((error) => {
        console.error("There was a problem with the fetch operation:", error);
      });

    // console.log("Clicked button");

    // fetch(url, {
    //   method: "POST",
    //   headers: {'Content-Type': 'application/json'},
    //   body: JSON.stringify({
    //     room: 1,
    //     temperature: 23,
    //   }),
    // })
    //   .then((res) => res.json())
    //   .then((data) => console.log(data));
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
