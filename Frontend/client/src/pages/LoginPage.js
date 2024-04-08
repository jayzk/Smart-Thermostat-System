import { useNavigate } from "react-router-dom";
import React, { useState } from "react";

/**
 * Functional component representing the login page.
 * Allows users to input a room number and password
 */
function LoginPage() {
  // Initialize state variables for room number and password
  const [roomNum, setRoomNum] = useState(0); 
  const [password, setPassword] = useState("");

  // Event handler for updating room number state
  const handleRoomNumChange = (event) => {
    setRoomNum(event.target.value); // Update roomNum state as user types
  };

  // Event handler for updating password state
  const handlePasswordChange = (event) => {
    setPassword(event.target.value);
  };

  // Initialize the useNavigate hook
  let navigate = useNavigate();

  // Function to handle login attempt
  const login = () => {
    // Check if room number is valid
    if(roomNum <= 0) {
      alert("Room number does not exist!");
    }
    // Check if password is correct
    else if(password !== "password") {
      alert("Incorrect password!")
    }
    // redirect to room page on successful login
    else {
      let path = `/room/${roomNum}`;
      navigate(path);
    }
  };

  // Render the login form
  return (
    <div class="container my-5 w-25">
      <form>
        <div class="mb-3">
          <label for="exampleInputEmail1" class="form-label">
            Room Number
          </label>
          <input
            type="roomNum"
            class="form-control"
            id="exampleInputEmail1"
            aria-describedby="emailHelp"
            value={roomNum}
            onChange={handleRoomNumChange}
          />
        </div>
        <div class="mb-3">
          <label for="exampleInputPassword1" class="form-label">
            Password
          </label>
          <input
            type="password"
            class="form-control"
            id="exampleInputPassword1"
            value={password}
            onChange={handlePasswordChange}
          />
        </div>
        <button type="submit" class="btn btn-primary" onClick={login}>
          Submit
        </button>
      </form>
    </div>
  );
}

export default LoginPage;
