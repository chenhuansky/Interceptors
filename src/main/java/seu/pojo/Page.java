package seu.pojo;

import lombok.Data;

@Data
public class Page {

    private int currentPage;

    /**
     * 每页显示条数
     */
    private int pageNumber ;
}
