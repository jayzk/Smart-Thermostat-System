import React from "react";
import ReactDOM from "react-dom/client";
import "./index.css";
import LoginPage from "./pages/LoginPage";
import reportWebVitals from "./reportWebVitals";
import "bootstrap/dist/css/bootstrap.min.css";
import RoomPage from "./pages/RoomPage";
import { createBrowserRouter, RouterProvider, Route } from "react-router-dom";
import { Navigate } from "react-router-dom";

// Create the router
const router = createBrowserRouter([
  {
    //define route for the login page
    path: "login",
    element: <LoginPage />,
  },
  {
    //define route for individual room pages
    path: "room/:roomNum",
    element: <RoomPage />,
  },
  {
    //default route
    path: "/",
    element: <Navigate to="login" />,
  },
]);

//create root element
const root = ReactDOM.createRoot(document.getElementById("root"));

//render the router element
root.render(
  <RouterProvider router={router} />
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
