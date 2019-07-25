package com.nis;

import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.service.impl.DeteTypeInfoServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DMLTest {
    @Autowired
    private DeteTypeInfoServiceImpl deteTypeInfoService;
    @Test
    public void createTable(){
        DeteTypeInfoEntity type = new DeteTypeInfoEntity();

        type.setOperator(1L);
        type.setOpTime(new Date());
        type.setTableName("ens14");
        List<DeteTypeMetaEntity> metas=new ArrayList<DeteTypeMetaEntity>();
//        "id": 16,
//                "name": "efg",
//                "typeId": 15,
//                "fieldType": "3",
//                "dataType": "3",
//                "dataLenth": 24,
//                "sort": 1,
//                "oid": "",
//                "unit": "",
//                "dicType": 2,
//                "i18nCode": "",
//                "remark": "3333",
//                "delFlag": "0"
        DeteTypeMetaEntity meta1 = new DeteTypeMetaEntity();
        meta1.setTypeId(14L);
        meta1.setFieldName("aaa");
        meta1.setDataType("1");
        meta1.setFieldType("1");
        meta1.setRemark("3333333333333333");
        meta1.setSort(1);
        meta1.setDelFlag("0");

        DeteTypeMetaEntity meta2 = new DeteTypeMetaEntity();
        meta2.setFieldName("bbb");
        meta2.setDataType("1");
        meta2.setFieldType("1");
        meta2.setSort(2);
        meta2.setTypeId(14L);
        meta2.setDelFlag("0");

        metas.add(meta1);
        metas.add(meta2);

        type.setDeteTypeMetas(metas);



        deteTypeInfoService.getBaseMapper().createTable(type);
    }
    @Test
    public void checkSetExists(){
        boolean b = deteTypeInfoService.checkDetectSetConfigExists(11L);

        System.out.println(b);
    }
    @Test
    public void testAlert(){
        DeteTypeInfoEntity type = new DeteTypeInfoEntity();
        type.setId(18L);
        type.setName("test1");
        type.setOperator(1L);
        type.setOpTime(new Date());
        type.setTableName("DI_ENC16");
        List<DeteTypeMetaEntity> metas=new ArrayList<DeteTypeMetaEntity>();
        DeteTypeMetaEntity meta1 = new DeteTypeMetaEntity();
        meta1.setId(22L);
        meta1.setName("test1");
        meta1.setFieldName("f_EERTE");
        meta1.setDataType("2");
        meta1.setFieldType("2");
        meta1.setDataLenth(23);
        meta1.setRemark("xxxxxx");
        meta1.setSort(1);
        meta1.setDelFlag("0");
        meta1.setOperator(1L);
        meta1.setOpTime(new Date());


        DeteTypeMetaEntity meta2 = new DeteTypeMetaEntity();
        meta2.setName("bbb");
        meta2.setFieldName("f_bbb");
        meta2.setFieldType("1");
        meta2.setDataType("1");
        meta2.setDataLenth(23);
        meta2.setSort(2);
        meta2.setTypeId(18L);
        meta2.setDelFlag("0");
        meta2.setOperator(1L);
        meta2.setOpTime(new Date());
        metas.add(meta1);
        metas.add(meta2);

        type.setDeteTypeMetas(metas);

        deteTypeInfoService.updateEntity(type);
    }
}
