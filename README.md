# Interceptors
简单mybatis分页拦截器实现物理分页
类似于之前写的切面，拦截器一样是在sql请求给出去之前进行拦截修改，将new_sql丢给数据库解析，可以说@Aspect是切入方法，@Intercepts则是切入请求

首先看Mapper和对应的XML

```java
//首先是StudentMapper文件，这里的接口就是真正执行sql语句的 
//我们要做的其实是在sql到达这里之前，将其改变
package seu.mapper;
import seu.pojo.Page;
import seu.pojo.Student;

import java.util.List;

public interface StudentMapper {
    public List<Student> queryPersonsByPage(Page page);
}


```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="seu.mapper.StudentMapper">
    <select id="queryPersonsByPage" parameterType="Page" resultType="Student">
        select * from student;
    </select>
</mapper>
```

以及数据库配置

```properties
driver = com.mysql.jdbc.Driver
url = jdbc:mysql://localhost:3306/mybatis?useSSL=false&useUnicode=true&characterEncoding=UTF-8
username = root
password = 123456

```

Student表的创建

```sql
CREATE TABLE `student` (
  `id` int(10) NOT NULL,
  `name` varchar(30) DEFAULT NULL,
  `tid` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8

```

插入数据

```sql
lisi	1
李四	 2
lisi	1
张散散	1
```

接下来配置mybatis-config.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!--可以在peoperty中配置属性但是默认以外部配置为最高优先级-->
    <properties resource="db.properties">
    </properties>
    <settings>
  <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>
    <typeAliases>
        <typeAlias type="seu.pojo.Student" alias="student01"/>
        <package name="seu.pojo"/>
    </typeAliases>
    <plugins>
        <plugin interceptor="seu.pojo.TestIntercepter">
            <property name="testProp" value="100"></property>
        </plugin>
    </plugins>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${driver}"/>
                <property name="url" value="${url}"/>
                <property name="username" value="${username}"/>
                <property name="password" value="${password}"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <!--resource绑定的路径用/-->
    <mapper class="seu.mapper.StudentMapper" />
        <mapper class="seu.mapper.TeacherMapper"/>

    </mappers>
</configuration>
```

然后是pojo

首先是页码对象Page

```java
package seu.pojo;

import lombok.Data;

/**
 * 分页对应的实体类
 */
@Data
public class Page {

    private int currentPage;

    /**
     * 每页显示条数
     */
    private int pageNumber ;

}
```

以及student对象

```java
package seu.pojo;

import lombok.Data;

@Data
public class Student {
    private int id;
    private String name;
}

```

最后是拦截器

```java
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
```

以及封装了一个Mybatis的操作工厂

```java
package seu.utils;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

//sqlSessionFactory
public class MybatisUtils {
    private static  SqlSessionFactory sqlSessionFactory;
   static{
       try {
           //使用mybatis第一步 获取sqlSessionFactory对象
           //把资源和配置文件加载进来并获取一个可操作的对象
           String resource="mybatis-config.xml";
           InputStream inputStream= Resources.getResourceAsStream(resource);
           sqlSessionFactory=new SqlSessionFactoryBuilder().build(inputStream);

       } catch (IOException e) {
           e.printStackTrace();
       }

   }
   public static SqlSession getSession(){
       return sqlSessionFactory.openSession();
   }
}
```

最后是测试文件

```java
 @Test

    public void test03(){
        Page page=new Page();
        SqlSession sqlSession = MybatisUtils.getSession();
        StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
        page.setCurrentPage(2);
        page.setPageNumber(2);
        //System.out.println("-u.getCount()------"+u.getCount());
        List<Student> l=studentMapper.queryPersonsByPage(page);
        System.out.println(l.get(1).getName());

    }
```

![image-20201213211915123](/Users/mac/Library/Application Support/typora-user-images/image-20201213211915123.png)

pom主要依赖

```xml
 <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.16.10</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>3.4.6</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.47</version>
        </dependency>
```


