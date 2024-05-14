import React, { FormEvent, useEffect, useRef, useState } from "react";
import { useHistory } from "react-router-dom";

const API_URL = process.env.DBHUB_API_URL

export default function Configs() {
    const [collector, setCollector] = useState<string>(null)
    const [collectorError, setCollectorError] = useState<string>("")
    const [sources, setSources] = useState<string>(null)
    const [sourcesError, setSourcesError] = useState<string>("")

    const collectorField = useRef<HTMLTextAreaElement>(null)
    const sourcesField = useRef<HTMLTextAreaElement>(null)

    const history = useHistory()

    useEffect(
        () => {
            fetch(
                API_URL + '/admin/collector-config/collector',
                {method: 'GET', credentials: 'include'}
            )
                .then(response => {
                    if (response.status == 401 || response.status == 403) {
                        history.push("/login")
                        return Promise.reject()
                    } else if (!response.ok) {
                        return Promise.reject()
                    }
                    return response.text()
                })
                .then(text => {
                    setCollector(text)
                })
        },
        []
    );

    useEffect(
        () => {
            fetch(
                API_URL + '/admin/collector-config/sources',
                {method: 'GET', credentials: 'include'}
            )
                .then(response => {
                    if (response.status == 401 || response.status == 403) {
                        history.push("/login")
                        return Promise.reject()
                    } else if (!response.ok) {
                        return Promise.reject()
                    }
                    return response.text()
                })
                .then(text => {
                    setSources(text)
                })
        },
        []
    );

    const logout = () => {
        setCollector(null)
        setSources(null)

        fetch(
            API_URL + '/logout',
            {method: 'POST', credentials: 'include'}
        )
            .then(request => {
                if (request.ok) {
                    history.push('/')
                }
            })
    };

    const resetCollector = (event: FormEvent) => {
        event.preventDefault()
        fetch(
            API_URL + '/admin/collector-config/collector',
            {
                method: "POST",
                body: collector,
                headers: {'Content-Type': 'application/json;charset=utf-8'},
                credentials: 'include',
            }
        )
            .then(response => {
                if (response.status == 401 || response.status == 403) {
                    history.push("/login")
                } else if (response.status == 400) {
                    setCollectorError("Неверный формат")
                } else if (!response.ok) {
                    setCollectorError("Ошибка")
                } else {
                    setCollectorError("")
                }
            })
    }

    const resetSources = (event: FormEvent) => {
        event.preventDefault()
        fetch(
            API_URL + '/admin/collector-config/sources',
            {
                method: "PUT",
                body: sources,
                headers: {'Content-Type': 'application/json;charset=utf-8'},
                credentials: 'include',
            }
        )
            .then(response => {
                if (response.status == 401 || response.status == 403) {
                    history.push("/login")
                } else if (response.status == 400) {
                    setSourcesError("Неверный формат")
                } else if (!response.ok) {
                    setSourcesError("Ошибка")
                } else {
                    setSourcesError("")
                }
            })
    }

    const onCollectorInput = () => {
        setCollector(collectorField.current.value)
    }

    const onSourcesInput = () => {
        setSources(sourcesField.current.value)
    }

    return (
        <>
            <div className="formwrapper">
                {sources != null &&
                    <form name="sources" onSubmit={resetSources}>
                        <textarea ref={sourcesField} onInput={onSourcesInput} defaultValue={sources}></textarea>
                        <button type="submit">Сохранить</button>
                    </form>
                }
                <div className="formerror">{sourcesError}</div>
            </div>
            <br />
            <div className="formwrapper">
                {collector != null &&
                    <form name="collector" onSubmit={resetCollector}>
                        <textarea ref={collectorField} onInput={onCollectorInput} defaultValue={collector}></textarea>
                        <button type="submit">Сохранить</button>
                    </form>
                }
                <div className="formerror">{collectorError}</div>
            </div>
            <br />
            <button className="logoutbutton" onClick={logout}>Выйти</button>
        </>
    )
}
