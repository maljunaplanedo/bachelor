import React, { useEffect, useRef, useState } from "react";

interface Article {
    id: number,
    source: string,
    link: string,
    title: string,
    text: string,
    timestamp: number,
}

interface ArticleAndInfo {
    article: Article,
    isShown: boolean,
}

interface ArticleToShow {
    article: Article,
    isShown: boolean,
    isTop: boolean,
    idx: number,
}

const NO_BOUND_ID = -1
const UPDATE_INTERVAL = 5000
const PAGE_ARTICLES_COUNT = 10
const MAX_ARTICLES_IN_UPDATE_REQUEST = 20
const API_URL = process.env.DBHUB_API_URL

interface Top {
    articles: ArticleAndInfo[],
    loadingNow: boolean,
    boundId: number,
}

interface Pages {
    articles: ArticleAndInfo[],
    nextPage: number,
    loadingNow: boolean,
    boundId: number,
}

interface ArticlesAndBoundId {
    articles: Article[],
    boundId: number,
}

export default function Feed() {
    const [top, setTop] = useState<Top>({
        articles: [],
        loadingNow: false,
        boundId: NO_BOUND_ID
    })

    const [pages, setPages] = useState<Pages>({
        articles: [],
        nextPage: 0,
        loadingNow: false,
        boundId: NO_BOUND_ID
    })

    const loadTop = () => {
        if (top.loadingNow) {
            return;
        }

        if (pages.boundId == NO_BOUND_ID) {
            return
        }

        setTop({
            ...top,
            loadingNow: true,
        })

        let url = API_URL + '/articles/after?'
        url += 'boundId=' + (top.boundId == NO_BOUND_ID ? pages.boundId : top.boundId)
        url += '&limit=' + MAX_ARTICLES_IN_UPDATE_REQUEST

        fetch(url)
            .then(response => {
                if (!response.ok) {
                    return Promise.reject();
                }
                return response.json();
            })
            .then(json => {
                const {articles: newArticles, boundId: newBoundId} = json as ArticlesAndBoundId;

                const newTop: Top = {
                    articles: top.articles,
                    boundId: newBoundId,
                    loadingNow: false,
                }
                newArticles.forEach(article => newTop.articles.push({article: article, isShown: false}))
                setTop(newTop)
            })
            .catch(() => {
                setTop({
                    ...top,
                    loadingNow: false,
                })
            })
    }

    const loadTopRef = useRef<() => void>(null);
    useEffect(() => {loadTopRef.current = loadTop}, [top, pages]);
    useEffect(() => {setInterval(() => loadTopRef.current(), UPDATE_INTERVAL)}, []);

    const loadNextPage = () => {
        if (pages.loadingNow) {
            return;
        }
        setPages({
            ...pages,
            loadingNow: true
        })

        let url = API_URL + '/articles/page?'
        url += 'count=' + PAGE_ARTICLES_COUNT
        url += '&page=' + pages.nextPage
        url += '&boundId=' + pages.boundId

        fetch(url)
            .then(response => {
                if (!response.ok) {
                    return Promise.reject();
                }
                return response.json();
            })
            .then(json => {
                const {articles: newArticles, boundId: newBoundId} = json as ArticlesAndBoundId;

                const newPages: Pages = {
                    articles: pages.articles,
                    nextPage: pages.nextPage + 1,
                    loadingNow: false,
                    boundId: newBoundId,
                }
                newArticles.forEach(article => newPages.articles.push({article, isShown: false}))
                setPages(newPages)
            })
            .catch(() => {
                setPages({
                    ...pages,
                    loadingNow: false,
                })
            })
    }

    useEffect(loadNextPage, [])

    let articlesToShow: ArticleToShow[] = []
    for (let idx = 0; idx < pages.articles.length; ++idx) {
        articlesToShow.push({article: pages.articles[idx].article, isShown: pages.articles[idx].isShown, isTop: false, idx: idx})
    }
    for (let idx = 0; idx < top.articles.length; ++idx) {
        articlesToShow.push({article: top.articles[idx].article, isShown: top.articles[idx].isShown, isTop: true, idx: idx})
    }
    articlesToShow.sort((article1, article2) => article2.article.timestamp - article1.article.timestamp)

    const toggleArticle = (idx: number, isTop: boolean) => {
        const doToggle = (articles: ArticleAndInfo[]) => {
            let result = [...articles];
            result[idx].isShown = !result[idx].isShown;
            return result;
        }

        if (isTop) {
            setTop({...top, articles: doToggle(top.articles)})
        } else {
            setPages({...pages, articles: doToggle(pages.articles)})
        }
    };

    const getPreparedArticleText = (text: string) => {
        const trimmed = text.trim().slice(0, 1000);
        let dots = 0;
        for (let idx = trimmed.length - 1; idx >= 0; --idx) {
            if (trimmed[idx] != '.') {
                break;
            }
            ++dots;
        }

        dots = Math.min(3, dots);
        return trimmed + ".".repeat(3 - dots);
    };

    return (
        <div className="formwrapper verywideformwrapper">
            <ul className="articleslist">
                {
                    articlesToShow.map(
                        (article, idx) =>
                            <li key={idx} className={"article " + (article.isShown ? "shownarticle" : "")}>
                                <div className="articletitle" onClick={() => toggleArticle(article.idx, article.isTop)}>
                                    <div className="articletitledate">{new Date(article.article.timestamp * 1000).toLocaleString("ru-RU")}</div>
                                    <div className="articletitletitle">
                                        <span>{article.article.source}: </span>{article.article.title}
                                    </div>
                                </div>
                                <div className="articletext">
                                    <p>{getPreparedArticleText(article.article.text)}</p>
                                    <a href={article.article.link} target="_blank" rel="noopener noreferrer">
                                        <button>Открыть</button>
                                    </a>
                                </div>
                            </li>
                    )
                }
            </ul>
            <button onClick={loadNextPage}>Еще</button>
        </div>
    )
}
