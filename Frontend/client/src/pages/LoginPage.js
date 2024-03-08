import { useNavigate } from "react-router-dom";
import React, { useState } from "react";

function LoginPage() {
  const [roomNum, setRoomNum] = useState(0); // Initialize roomNum state

  const handleRoomNumChange = (event) => {
    setRoomNum(event.target.value); // Update roomNum state as user types
  };

  let navigate = useNavigate();
  const login = () => {
    if(roomNum <= 0) {
      alert("Room number does not exist!");
    }
    else {
      let path = `/room/${roomNum}`;
    navigate(path);
    }
  };

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
