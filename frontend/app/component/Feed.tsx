import React, { useEffect, useRef, useState } from "react";

interface Article {
    id: number,
    source: string,
    link: string,
    title: string,
    text: string,
    timestamp: number,
}

const NO_BOUND_ID = -1

interface Top {
    articles: Article[],
    loadingNow: boolean,
    boundId: number,
}

interface Pages {
    articles: Article[],
    nextPage: number,
    loadingNow: boolean,
    boundId: number,
}

interface ArticlesAndBoundId {
    articles: Article[],
    boundId: number,
}

export default function Feed() {
    const PAGE_ARTICLES_COUNT = 2;

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

        let url = process.env.DBHUB_API_URL + '/articles/after?';
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
                    newTop.articles.push(newArticles[idx])
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
    useEffect(() => {setInterval(() => loadTopRef.current(), 5000)}, []);

    const loadNextPage = () => {
        if (pages.loadingNow) {
            return;
        }
        setPages({
            ...pages,
            loadingNow: true
        })

        let url = process.env.DBHUB_API_URL + '/articles/page?'
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
                newArticles.forEach(article => newPages.articles.push(article))
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

    let topArticlesReversed: Article[] = []
    for (let idx = top.articles.length - 1; idx >= 0; --idx) {
        topArticlesReversed.push(top.articles[idx])
    }

    const showArticles = (articles: Article[], addToKey: number) => {
        return articles.map(
            (article, idx) =>
                <li key={idx + addToKey}>
                    <a href={article.link} target="_blank" rel="noopener noreferrer">{article.title}</a>
                </li>
        );
    }

    return (
        <>
            <ul>
                { showArticles(topArticlesReversed, 0) }
                { showArticles(pages.articles, topArticlesReversed.length) }
            </ul>
            <button onClick={loadNextPage}>Еще</button>
        </>
    )
}
