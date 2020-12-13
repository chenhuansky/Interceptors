package seu.mapper;

import seu.pojo.Page;
import seu.pojo.Student;


import java.util.List;

public interface StudentMapper {


        public List<Student> queryPersonsByPage(Page page);

}
