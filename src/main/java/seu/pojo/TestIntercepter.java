package seu.pojo;

import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.Properties;
@Intercepts(value = { @Signature(args = { Connection.class ,Integer.class}, method = "prepare", type = StatementHandler.class) })
public class TestIntercepter implements Interceptor {
    @Autowired
    public Page page;
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        // TODO Auto-generated method stub
        RoutingStatementHandler statementHandler = (RoutingStatementHandler) invocation.getTarget();
        MetaObject metaObject=MetaObject.forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY,SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
        MappedStatement mappedStatement=(MappedStatement) metaObject.getValue("delegate.mappedStatement");

        String id=mappedStatement.getId();
        if(id.matches(".+ByPage$")){
            System.out.println("方法已经拦截");
            BoundSql boundSql=statementHandler.getBoundSql();
            String sql0=boundSql.getSql();
            String sql=sql0.substring(0,sql0.length()-1);
            Page page=(Page) boundSql.getParameterObject();
            String newSql=sql+" limit " + page.getCurrentPage()+"," +page.getPageNumber();
            System.out.println(newSql);
            metaObject.setValue("delegate.boundSql.sql",newSql);
        }

        return invocation.proceed();
    }
    @Override
    public Object plugin(Object target) {
        // TODO Auto-generated method stub
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }
    @Override
    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub

    }

}