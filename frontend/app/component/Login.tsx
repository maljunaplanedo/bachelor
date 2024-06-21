import React, { FormEvent, useRef, useState } from "react";
import { useHistory } from "react-router-dom";

const API_URL = process.env.DBHUB_API_URL

interface UsernameAndPassword {
    username: string,
    password: string,
}

interface State {
    usernameAndPassword: UsernameAndPassword,
    loading: boolean,
    error: string,
}

export default function Login() {
    const [state, setState] = useState<State>({
        usernameAndPassword: {username: "", password: "",},
        loading: false,
        error: "",
    })

    const usernameField = useRef<HTMLInputElement>(null)
    const passwordField = useRef<HTMLInputElement>(null)

    const history = useHistory()

    const onInput = () => {
        setState({
            ...state,
            usernameAndPassword: {
                username: usernameField.current.value,
                password: passwordField.current.value,
            }
        })
    }

    const onSubmit = (event: FormEvent) => {
        event.preventDefault()
        if (state.loading) {
            return
        }

        setState({
            ...state,
            loading: true,
        })

        let url = API_URL + '/api/login'
        fetch(
            url,
            {
                method: "POST",
                body: JSON.stringify(state.usernameAndPassword),
                headers: {'Content-Type': 'application/json;charset=utf-8'},
                credentials: "include"
            }
        )
            .then(response => {
                const newState: State = {...state, loading: false}
                if (response.status == 403) {
                    newState.error = "Неверный логин/пароль"
                } else if (!response.ok) {
                    newState.error = "Ошибка"
                } else {
                    history.push("/configs")
                    return
                }
                setState(newState)
            })
            .catch(() => {
                setState({...state, loading: false, error: "Ошибка"})
            })

        return
    };

    return (
        <div className="formwrapper">
            <form name="login" onSubmit={onSubmit}>
                <input type="text" ref={usernameField} onInput={onInput} placeholder="Логин" disabled={state.loading} />
                <input type="password" ref={passwordField} onInput={onInput} placeholder="Пароль" disabled={state.loading} />
                <button type="submit" disabled={state.loading}>Войти</button>
            </form>
            <div className="formerror">{state.error}</div>
        </div>
    )
}
