import { useNavigate } from "react-router-dom";

function LoginPage() {
  let navigate = useNavigate();
  const login = () =>{ 
    let path = "/room"; 
    navigate(path);
  }

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
