package org.sonarcrypto.utils.jimple.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArgumentMappingTest {

  @Test
  void getters() {
    var position = new SourcePosition(10, 10, 5, 20);
    var mapping = new ArgumentMapping(2, position, false);

    assertThat(mapping.getArgIndex()).isEqualTo(2);
    assertThat(mapping.getSourcePosition()).isEqualTo(position);
    assertThat(mapping.isApproximated()).isFalse();
  }

  @Test
  void toString_includesAllFields() {
    var position = new SourcePosition(10, 10, 5, 20);
    var mapping = new ArgumentMapping(1, position, true);

    assertThat(mapping.toString())
        .contains("argIndex=1")
        .contains("approximated=true")
        .contains(position.toString());
  }

  @Test
  void equals_andHashCode() {
    var pos = new SourcePosition(10, 10, 5, 20);
    var a = new ArgumentMapping(1, pos, true);
    var b = new ArgumentMapping(1, pos, true);
    var differentIndex = new ArgumentMapping(2, pos, true);
    var differentApproximated = new ArgumentMapping(1, pos, false);
    var differentPos = new ArgumentMapping(1, new SourcePosition(99, 99, 0, 0), true);

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());

    assertThat(a).isNotEqualTo(differentIndex);
    assertThat(a).isNotEqualTo(differentApproximated);
    assertThat(a).isNotEqualTo(differentPos);
    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("other type");
  }
}
