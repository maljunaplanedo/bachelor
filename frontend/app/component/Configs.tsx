import React, { FormEvent, useEffect, useRef, useState } from "react";
import { useHistory } from "react-router-dom";

const API_URL = process.env.DBHUB_API_URL

interface RequestResult {
    value: string,
    ok: boolean,
}

export default function Configs() {
    const [collector, setCollector] = useState<string>(null)
    const [collectorResult, setCollectorResult] = useState<RequestResult>({value: "", ok: true})
    const [sources, setSources] = useState<string>(null)
    const [sourcesResult, setSourcesResult] = useState<RequestResult>({value: "", ok: true})
    const [publisher, setPublisher] = useState<string>(null)
    const [publisherResult, setPublisherResult] = useState<RequestResult>({value: "", ok: true})

    const collectorField = useRef<HTMLTextAreaElement>(null)
    const sourcesField = useRef<HTMLTextAreaElement>(null)
    const publisherField = useRef<HTMLTextAreaElement>(null)

    const history = useHistory()

    useEffect(
        () => {
            fetch(
                API_URL + '/api/admin/collector-config/collector',
                {method: 'GET', credentials: 'include'}
            )
                .then(response => {
                    if (response.status == 401 || response.status == 403) {
                        history.push("/login")
                        return Promise.reject()
                    } else if (!response.ok) {
                        return Promise.reject()
                    }
                    return response.json()
                })
                .then(json => {
                    setCollector(JSON.stringify(json, null, 2))
                })
        },
        []
    );

    useEffect(
        () => {
            fetch(
                API_URL + '/api/admin/collector-config/sources',
                {method: 'GET', credentials: 'include'}
            )
                .then(response => {
                    if (response.status == 401 || response.status == 403) {
                        history.push("/login")
                        return Promise.reject()
                    } else if (!response.ok) {
                        return Promise.reject()
                    }
                    return response.json()
                })
                .then(json => {
                    setSources(JSON.stringify(json, null, 2))
                })
        },
        []
    );

    useEffect(
        () => {
            fetch(
                API_URL + '/api/admin/publisher-config/config',
                {method: 'GET', credentials: 'include'}
            )
                .then(response => {
                    if (response.status == 401 || response.status == 403) {
                        history.push("/login")
                        return Promise.reject()
                    } else if (!response.ok) {
                        return Promise.reject()
                    }
                    return response.json()
                })
                .then(json => {
                    setPublisher(JSON.stringify(json, null, 2))
                })
        },
        []
    );

    const logout = () => {
        setCollector(null)
        setSources(null)

        fetch(
            API_URL + '/api/logout',
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
            API_URL + '/api/admin/collector-config/collector',
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
                    setCollectorResult({value: "Неверный формат", ok: false})
                } else if (!response.ok) {
                    setCollectorResult({value: "Ошибка", ok: false})
                } else {
                    setCollectorResult({value: "Сохранено успешно", ok: true})
                }
            })
            .catch(() => {
                setCollectorResult({value: "Ошибка", ok: false})
            })
    }

    const resetSources = (event: FormEvent) => {
        event.preventDefault()
        fetch(
            API_URL + '/api/admin/collector-config/sources',
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
                    setSourcesResult({value: "Неверный формат", ok: false})
                } else if (!response.ok) {
                    setSourcesResult({value: "Ошибка", ok: false})
                } else {
                    setSourcesResult({value: "Сохранено успешно", ok: true})
                }
            })
            .catch(() => {
                setSourcesResult({value: "Ошибка", ok: false})
            })
    }

    const resetPublisher = (event: FormEvent) => {
        event.preventDefault()
        fetch(
            API_URL + '/api/admin/publisher-config/config',
            {
                method: "POST",
                body: publisher,
                headers: {'Content-Type': 'application/json;charset=utf-8'},
                credentials: 'include',
            }
        )
            .then(response => {
                if (response.status == 401 || response.status == 403) {
                    history.push("/login")
                } else if (response.status == 400) {
                    setPublisherResult({value: "Неверный формат", ok: false})
                } else if (!response.ok) {
                    setPublisherResult({value: "Ошибка", ok: false})
                } else {
                    setPublisherResult({value: "Сохранено успешно", ok: true})
                }
            })
            .catch(() => {
                setPublisherResult({value: "Ошибка", ok: false})
            })
    }

    const onCollectorInput = () => {
        setCollector(collectorField.current.value)
    }

    const onSourcesInput = () => {
        setSources(sourcesField.current.value)
    }

    const onPublisherInput = () => {
        setPublisher(publisherField.current.value)
    }

    return (
        <>
            <div className="formwrapper wideformwrapper">
                {sources != null &&
                    <form name="sources" onSubmit={resetSources}>
                        <div>Настройки источников новостей</div>
                        <textarea ref={sourcesField} onInput={onSourcesInput} defaultValue={sources}></textarea>
                        <button type="submit">Сохранить</button>
                    </form>
                }
                <div className={sourcesResult.ok ? "formsuccess" : "formerror"}>{sourcesResult.value}</div>
            </div>
            <br />
            <div className="formwrapper wideformwrapper">
                {collector != null &&
                    <form name="collector" onSubmit={resetCollector}>
                        <div>Общие настройки сбора новостей</div>
                        <textarea ref={collectorField} onInput={onCollectorInput} defaultValue={collector}></textarea>
                        <button type="submit">Сохранить</button>
                    </form>
                }
                <div className={collectorResult.ok ? "formsuccess" : "formerror"}>{collectorResult.value}</div>
            </div>
            <br />
            <div className="formwrapper wideformwrapper">
                {collector != null &&
                    <form name="publisher" onSubmit={resetPublisher}>
                        <div>Настройки поставки в Telegram</div>
                        <textarea ref={publisherField} onInput={onPublisherInput} defaultValue={publisher}></textarea>
                        <button type="submit">Сохранить</button>
                    </form>
                }
                <div className={publisherResult.ok ? "formsuccess" : "formerror"}>{publisherResult.value}</div>
            </div>
            <br />
            <button className="logoutbutton" onClick={logout}>Выйти</button>
        </>
    )
}
