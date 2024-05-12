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

const NO_BOUND_ID = -1
const UPDATE_INTERVAL = 5000
const PAGE_ARTICLES_COUNT = 2
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

        let url = API_URL + '/articles/after?';
        url += 'boundId=' + (top.boundId == NO_BOUND_ID ? pages.boundId : top.boundId);

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
                for (let idx = newArticles.length - 1; idx >= 0; --idx) {
                    newTop.articles.push({article: newArticles[idx], isShown: false})
                }
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

    let topArticlesReversed: ArticleAndInfo[] = []
    for (let idx = top.articles.length - 1; idx >= 0; --idx) {
        topArticlesReversed.push(top.articles[idx])
    }

    const toggleArticle = (idx: number, isTop: boolean) => {
        const doToggle = (idx: number, articles: ArticleAndInfo[]) => {
            let result = [...articles];
            result[idx].isShown = !result[idx].isShown;
            return result;
        }

        if (isTop) {
            setTop({...top, articles: doToggle(top.articles.length - idx, top.articles)})
        } else {
            setPages({...pages, articles: doToggle(idx, pages.articles)})
        }
    };

    const showArticles = (articles: ArticleAndInfo[], isTop: boolean) => {
        return articles.map(
            (article, idx) =>
                <li key={idx + (isTop ? 0 : top.articles.length)} className={"article " + (article.isShown ? "shownarticle" : "")}>
                    <div className="articletitle" onClick={() => toggleArticle(idx, isTop)}>
                        <div className="articletitledate">{new Date(article.article.timestamp * 1000).toLocaleString("ru-RU")}</div>
                        <div className="articletitletitle">
                            <span>{article.article.source}: </span>{article.article.title}
                        </div>
                    </div>
                    <div className="articletext">
                        <p>{article.article.text}</p>
                        <a href={article.article.link} target="_blank" rel="noopener noreferrer">
                            <button>Открыть</button>
                        </a>
                    </div>
                </li>
        );
    }

    return (
        <>
            <ul className="articleslist">
                { showArticles(topArticlesReversed, true) }
                { showArticles(pages.articles, false) }
            </ul>
            <button onClick={loadNextPage}>Еще</button>
        </>
    )
}
