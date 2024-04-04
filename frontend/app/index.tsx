import './style.css'
import {createRoot} from "react-dom/client";
import {BrowserRouter, Route} from "react-router-dom";
import Feed from "./component/Feed";
import React from 'react';

const root = createRoot(document.querySelector("#root"));

root.render(
    <BrowserRouter>
        <Route path="/" exact component={Feed} />
    </BrowserRouter>
)
