package org.sonarcrypto.utils.jimple.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArgumentMappingTest {

  @Test
  void getters() {
    var position = new SourcePosition(10, 10, 5, 20);
    var mapping = new ArgumentMapping(2, position);

    assertThat(mapping.getArgIndex()).isEqualTo(2);
    assertThat(mapping.getSourcePosition()).isEqualTo(position);
  }

  @Test
  void toString_includesAllFields() {
    var position = new SourcePosition(10, 10, 5, 20);
    var mapping = new ArgumentMapping(1, position);

    assertThat(mapping.toString()).contains("argIndex=1").contains(position.toString());
  }

  @Test
  void equals_andHashCode() {
    var pos = new SourcePosition(10, 10, 5, 20);
    var a = new ArgumentMapping(1, pos);
    var b = new ArgumentMapping(1, pos);
    var differentIndex = new ArgumentMapping(2, pos);
    var differentPos = new ArgumentMapping(1, new SourcePosition(99, 99, 0, 0));

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());

    assertThat(a).isNotEqualTo(differentIndex);
    assertThat(a).isNotEqualTo(differentPos);
    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("other type");
  }
}
