package ru.dbhub.jpa;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import ru.dbhub.StorageTransaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

class StorageTransactionImpl<A> implements StorageTransaction<A> {
    private class RollbackOnExceptionInvocationHandler implements InvocationHandler {
        private final A underlying;

        private RollbackOnExceptionInvocationHandler(A underlying) {
            this.underlying = underlying;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(underlying, args);
            } catch (Exception exception) {
                rollback();
                throw exception;
            }
        }
    }

    private final PlatformTransactionManager platformTransactionManager;

    private final TransactionStatus transactionStatus;

    private boolean finished = false;

    private final A accessor;

    @SuppressWarnings("unchecked")
    private A createRollbackOnExceptionAccessorProxy(Class<A> accessorInterface, A accessor) {
        return (A) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{ accessorInterface },
            new RollbackOnExceptionInvocationHandler(accessor)
        );
    }

    StorageTransactionImpl(
        Class<A> accessorInterface, A accessor, PlatformTransactionManager platformTransactionManager
    ) {
        this.platformTransactionManager = platformTransactionManager;
        this.transactionStatus = platformTransactionManager.getTransaction(null);
        this.accessor = createRollbackOnExceptionAccessorProxy(accessorInterface, accessor);
    }

    @Override
    public A get() {
        if (finished) {
            throw new IllegalStateException("Transaction is finished");
        }
        return accessor;
    }

    @Override
    public void commit() {
        platformTransactionManager.commit(transactionStatus);
        finished = true;
    }

    @Override
    public void rollback() {
        platformTransactionManager.rollback(transactionStatus);
        finished = true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
