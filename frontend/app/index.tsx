import './style.css'
import {createRoot} from "react-dom/client";
import {BrowserRouter, Route} from "react-router-dom";
import Feed from "./component/Feed";
import React from 'react';
import Login from './component/Login';
import Configs from './component/Configs';

const root = createRoot(document.querySelector("#root"));

root.render(
    <BrowserRouter>
        <Route path="/" exact component={Feed} />
        <Route path="/login" exact component={Login} />
        <Route path="/configs" exact component={Configs} />
    </BrowserRouter>
)
