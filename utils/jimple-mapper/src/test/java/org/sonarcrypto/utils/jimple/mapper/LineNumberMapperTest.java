package org.sonarcrypto.utils.jimple.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.model.FullPosition;
import sootup.core.model.Position;

class LineNumberMapperTest {

  @Test
  void testRecordClassPosition() {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");
    Position position = new FullPosition(10, 1, 20, 5);

    mapper.recordClassPosition(1, "com.example.TestClass", position);

    LineMappingCollection collection = mapper.getCollection();
    assertThat(collection.getClassName()).isEqualTo("com.example.TestClass");
    assertThat(collection.getSourceFileName()).isEqualTo("TestClass.java");
    assertThat(collection.size()).isEqualTo(1);

    LineMapping mapping = collection.getMappings().get(0);
    assertThat(mapping.getJimpleLine()).isEqualTo(1);
    assertThat(mapping.getElementType()).isEqualTo(ElementType.CLASS);
    assertThat(mapping.getElementSignature()).isEqualTo("com.example.TestClass");
    assertThat(mapping.getSourcePosition().getFirstLine()).isEqualTo(10);
    assertThat(mapping.getSourcePosition().getLastLine()).isEqualTo(20);
  }

  @Test
  void testRecordMethodPosition() {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");
    Position position = new FullPosition(15, 3, 25, 7);

    mapper.recordMethodPosition(5, "<com.example.TestClass: void test()>", position);

    LineMappingCollection collection = mapper.getCollection();
    assertThat(collection.size()).isEqualTo(1);

    LineMapping mapping = collection.getMappings().get(0);
    assertThat(mapping.getJimpleLine()).isEqualTo(5);
    assertThat(mapping.getElementType()).isEqualTo(ElementType.METHOD);
    assertThat(mapping.getElementSignature()).isEqualTo("<com.example.TestClass: void test()>");
  }

  @Test
  void testRecordFieldPosition() {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");
    Position position = new FullPosition(8, 5, 8, 20);

    mapper.recordFieldPosition(3, "<com.example.TestClass: int field>", position);

    LineMappingCollection collection = mapper.getCollection();
    assertThat(collection.size()).isEqualTo(1);

    LineMapping mapping = collection.getMappings().get(0);
    assertThat(mapping.getJimpleLine()).isEqualTo(3);
    assertThat(mapping.getElementType()).isEqualTo(ElementType.FIELD);
  }

  @Test
  void testRecordStmtPosition() {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");
    Position position = new FullPosition(18, 9, 18, 25);

    mapper.recordStmtPosition(10, "x = 5", position);

    LineMappingCollection collection = mapper.getCollection();
    assertThat(collection.size()).isEqualTo(1);

    LineMapping mapping = collection.getMappings().get(0);
    assertThat(mapping.getJimpleLine()).isEqualTo(10);
    assertThat(mapping.getElementType()).isEqualTo(ElementType.STATEMENT);
    assertThat(mapping.getElementSignature()).isEqualTo("x = 5");
  }

  @Test
  void testMultipleMappings() {
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");

    mapper.recordClassPosition(1, "com.example.TestClass", new FullPosition(1, 1, 30, 1));
    mapper.recordFieldPosition(3, "<com.example.TestClass: int x>", new FullPosition(5, 5, 5, 15));
    mapper.recordMethodPosition(
        5, "<com.example.TestClass: void test()>", new FullPosition(10, 3, 20, 4));
    mapper.recordStmtPosition(8, "x = 5", new FullPosition(15, 9, 15, 14));

    LineMappingCollection collection = mapper.getCollection();
    assertThat(collection.size()).isEqualTo(4);
    assertThat(collection.getSourceFileName()).isEqualTo("TestClass.java");
    assertThat(collection.getMappings().get(0).getElementType()).isEqualTo(ElementType.CLASS);
    assertThat(collection.getMappings().get(1).getElementType()).isEqualTo(ElementType.FIELD);
    assertThat(collection.getMappings().get(2).getElementType()).isEqualTo(ElementType.METHOD);
    assertThat(collection.getMappings().get(3).getElementType()).isEqualTo(ElementType.STATEMENT);
  }

  @Test
  void testNoPositionInformationIsRecorded() {
    // Even NoPositionInformation should be recorded for completeness
    LineNumberMapper mapper = new LineNumberMapper("com.example.TestClass");
    mapper.setSourceFileName("TestClass.java");
    Position noPosition = NoPositionInformation.getInstance();

    mapper.recordClassPosition(1, "com.example.TestClass", noPosition);

    LineMappingCollection collection = mapper.getCollection();
    assertThat(collection.size()).isEqualTo(1);

    LineMapping mapping = collection.getMappings().get(0);
    assertThat(mapping.getSourcePosition().getFirstLine()).isEqualTo(-1);
    assertThat(mapping.getSourcePosition().getLastLine()).isEqualTo(-1);
  }
}
