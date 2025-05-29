// Example test file
describe('Basic test suite', () => {
  test('should pass a simple test', () => {
    expect(1 + 1).toBe(2);
  });

  test('should handle string operations', () => {
    const str = 'Hello, World!';
    expect(str.length).toBe(13);
    expect(str.toUpperCase()).toBe('HELLO, WORLD!');
  });
}); 