import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import seu.mapper.StudentMapper;
import seu.pojo.MybatisUtils;
import seu.pojo.Page;
import seu.pojo.Student;

import java.util.List;


public class Testmybatis {
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
}
