package org.printscript.syntax;

public interface TypeName {
  <R> R accept(TypeNameVisitor<R> visitor);

  TypeName NUMBER =
      new TypeName() {
        @Override
        public <R> R accept(TypeNameVisitor<R> visitor) {
          return visitor.visitNumber();
        }

        @Override
        public String toString() {
          return "NUMBER";
        }
      };

  TypeName STRING =
      new TypeName() {
        @Override
        public <R> R accept(TypeNameVisitor<R> visitor) {
          return visitor.visitString();
        }

        @Override
        public String toString() {
          return "STRING";
        }
      };

  TypeName BOOLEAN =
      new TypeName() {
        @Override
        public <R> R accept(TypeNameVisitor<R> visitor) {
          return visitor.visitBoolean();
        }

        @Override
        public String toString() {
          return "BOOLEAN";
        }
      };
}
